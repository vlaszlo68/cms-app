package hu.laci.cms.backend.config.database.migration;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class DatabaseMigrationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private static final String MIGRATION_PATH = "db/migration";
    private static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("^V(\\d+)__(.+)\\.sql$");
    private static final long MIGRATION_LOCK_ID = 7_420_251_001L;

    private DatabaseMigrationRunner() {
    }

    public static void runMigrations() {
        List<Migration> migrations = loadMigrations();
        if (migrations.isEmpty()) {
            LOGGER.info("No database migrations found under {}", MIGRATION_PATH);
            return;
        }

        try (Connection connection = DatabaseConfig.getConnection()) {
            acquireMigrationLock(connection);
            try {
                ensureSchemaMigrationsTable(connection);
                Map<Integer, AppliedMigration> appliedMigrations = loadAppliedMigrations(connection);

                for (Migration migration : migrations) {
                    AppliedMigration appliedMigration = appliedMigrations.get(migration.version());
                    if (appliedMigration != null) {
                        validateAppliedMigration(connection, migration, appliedMigration);
                        LOGGER.info("Skipping already applied database migration {}", migration.scriptName());
                        continue;
                    }

                    applyMigration(connection, migration);
                }
            } finally {
                releaseMigrationLock(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to run database migrations.", e);
        }
    }

    private static void acquireMigrationLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_lock(?)")) {
            statement.setLong(1, MIGRATION_LOCK_ID);
            statement.execute();
            LOGGER.info("Acquired database migration lock {}", MIGRATION_LOCK_ID);
        }
    }

    private static void releaseMigrationLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, MIGRATION_LOCK_ID);
            statement.execute();
            LOGGER.info("Released database migration lock {}", MIGRATION_LOCK_ID);
        }
    }

    private static void ensureSchemaMigrationsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version INTEGER PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        script_name VARCHAR(255) NOT NULL UNIQUE,
                        checksum VARCHAR(64),
                        applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("ALTER TABLE schema_migrations ADD COLUMN IF NOT EXISTS checksum VARCHAR(64)");
        }
    }

    private static Map<Integer, AppliedMigration> loadAppliedMigrations(Connection connection) throws SQLException {
        java.util.HashMap<Integer, AppliedMigration> appliedMigrations = new java.util.HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT version, script_name, checksum FROM schema_migrations ORDER BY version");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int version = resultSet.getInt("version");
                appliedMigrations.put(version, new AppliedMigration(
                        version,
                        resultSet.getString("script_name"),
                        resultSet.getString("checksum")));
            }
        }

        return appliedMigrations;
    }

    private static void validateAppliedMigration(Connection connection, Migration migration,
                                                 AppliedMigration appliedMigration) throws SQLException {
        if (!migration.scriptName().equals(appliedMigration.scriptName())) {
            throw new SQLException("Applied migration version " + migration.version()
                    + " was recorded with script " + appliedMigration.scriptName()
                    + " but current script is " + migration.scriptName());
        }

        if (appliedMigration.checksum() == null || appliedMigration.checksum().isBlank()) {
            backfillChecksum(connection, migration);
            return;
        }

        if (!migration.checksum().equals(appliedMigration.checksum())) {
            throw new SQLException("Checksum mismatch for applied migration " + migration.scriptName()
                    + ". Recorded checksum=" + appliedMigration.checksum()
                    + ", current checksum=" + migration.checksum());
        }
    }

    private static void backfillChecksum(Connection connection, Migration migration) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE schema_migrations
                SET checksum = ?
                WHERE version = ? AND checksum IS NULL
                """)) {
            statement.setString(1, migration.checksum());
            statement.setInt(2, migration.version());
            statement.executeUpdate();
            LOGGER.info("Backfilled checksum for database migration {}", migration.scriptName());
        }
    }

    private static void applyMigration(Connection connection, Migration migration) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            LOGGER.info("Applying database migration {}", migration.scriptName());
            for (String statementSql : splitSqlStatements(migration.sql())) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementSql);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO schema_migrations (version, description, script_name, checksum)
                    VALUES (?, ?, ?, ?)
                    """)) {
                statement.setInt(1, migration.version());
                statement.setString(2, migration.description());
                statement.setString(3, migration.scriptName());
                statement.setString(4, migration.checksum());
                statement.executeUpdate();
            }

            connection.commit();
            LOGGER.info("Applied database migration {}", migration.scriptName());
        } catch (SQLException e) {
            connection.rollback();
            LOGGER.error("Failed to apply database migration {}", migration.scriptName(), e);
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        for (String statement : sql.split(";")) {
            String trimmedStatement = statement.trim();
            if (!trimmedStatement.isEmpty()) {
                statements.add(trimmedStatement);
            }
        }

        return statements;
    }

    private static List<Migration> loadMigrations() {
        try {
            URL resourceUrl = getClassLoader().getResource(MIGRATION_PATH);
            if (resourceUrl == null) {
                return List.of();
            }

            URI resourceUri = resourceUrl.toURI();
            if ("jar".equals(resourceUri.getScheme())) {
                try (FileSystem fileSystem = FileSystems.newFileSystem(resourceUri, Map.of())) {
                    return readMigrations(fileSystem.getPath(MIGRATION_PATH));
                } catch (FileSystemAlreadyExistsException e) {
                    FileSystem fileSystem = FileSystems.getFileSystem(resourceUri);
                    return readMigrations(fileSystem.getPath(MIGRATION_PATH));
                }
            }

            return readMigrations(Path.of(resourceUri));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to load database migrations.", e);
        }
    }

    private static List<Migration> readMigrations(Path migrationDirectory) throws IOException {
        try (Stream<Path> paths = Files.list(migrationDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(DatabaseMigrationRunner::toMigration)
                    .sorted(Comparator.comparingInt(Migration::version))
                    .toList();
        }
    }

    private static Migration toMigration(Path path) {
        String scriptName = path.getFileName().toString();
        Matcher matcher = MIGRATION_FILE_PATTERN.matcher(scriptName);
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid migration file name: " + scriptName);
        }

        try {
            int version = Integer.parseInt(matcher.group(1));
            String description = matcher.group(2).replace('_', ' ');
            String sql = Files.readString(path, StandardCharsets.UTF_8);
            return new Migration(version, description, scriptName, sql, checksum(sql));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read migration file: " + scriptName, e);
        }
    }

    private static String checksum(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 checksum algorithm is not available.", e);
        }
    }

    private static ClassLoader getClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }

        return DatabaseMigrationRunner.class.getClassLoader();
    }

    private record Migration(int version, String description, String scriptName, String sql, String checksum) {
    }

    private record AppliedMigration(int version, String scriptName, String checksum) {
    }
}
