package hu.laci.cms.backend.config.database.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspects and narrowly repairs an applied migration checksum when the only difference is line endings.
 * <p>
 * This helper is intentionally not called by application startup. Operators must first inspect the result,
 * validate the migration against an isolated database, and explicitly invoke the guarded repair using the
 * target database connection. Content changes and ambiguous checksum differences are always rejected.
 */
public final class MigrationChecksumRepairHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationChecksumRepairHelper.class);
    private static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("^V(\\d+)__(.+)\\.sql$");

    private MigrationChecksumRepairHelper() {
    }

    /**
     * Reads the applied migration row and compares its recorded checksum with the supplied migration file.
     *
     * @param connection explicit connection to the database whose migration history is being inspected
     * @param migrationFile exact versioned migration file to inspect
     * @return immutable inspection data, including raw and normalized-line-ending checksums
     * @throws IOException if the migration file cannot be read
     * @throws SQLException if the migration history cannot be queried
     * @throws IllegalArgumentException if the file name is not a versioned migration name or its row is absent
     */
    public static ChecksumInspection inspect(Connection connection, Path migrationFile) throws IOException, SQLException {
        MigrationFile migration = readMigrationFile(migrationFile);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT script_name, checksum
                FROM schema_migrations
                WHERE version = ?
                """)) {
            statement.setInt(1, migration.version());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new IllegalArgumentException("No applied migration row exists for version "
                            + migration.version());
                }

                String recordedScriptName = rows.getString("script_name");
                if (!migration.scriptName().equals(recordedScriptName)) {
                    throw new IllegalArgumentException("Migration file " + migration.scriptName()
                            + " does not match recorded script " + recordedScriptName);
                }

                String recordedChecksum = rows.getString("checksum");
                return new ChecksumInspection(migration.version(), migration.scriptName(), recordedChecksum,
                        checksum(migration.content()), checksum(normalizeLineEndings(migration.content(), "\n")),
                        checksum(normalizeLineEndings(migration.content(), "\r\n")));
            }
        }
    }

    /**
     * Replaces a recorded checksum only when inspection proved that line endings are the sole difference.
     * <p>
     * The update includes the inspected checksum in its {@code WHERE} clause, so concurrent or unexpected
     * migration-history changes cannot be overwritten.
     *
     * @param connection explicit connection to the database whose migration history is being repaired
     * @param inspection previously obtained inspection result for the exact migration to repair
     * @throws SQLException if the guarded history update cannot be performed
     * @throws IllegalStateException if the mismatch is not line-ending-only or the guarded update affects
     *                               anything other than one row
     */
    public static void repairLineEndingOnlyMismatch(Connection connection, ChecksumInspection inspection)
            throws SQLException {
        if (!inspection.isLineEndingOnlyMismatch()) {
            throw new IllegalStateException("Refusing to repair migration " + inspection.scriptName()
                    + " because its checksum mismatch is not explained solely by line endings.");
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE schema_migrations
                SET checksum = ?
                WHERE version = ? AND script_name = ? AND checksum = ?
                """)) {
            statement.setString(1, inspection.currentChecksum());
            statement.setInt(2, inspection.version());
            statement.setString(3, inspection.scriptName());
            statement.setString(4, inspection.recordedChecksum());
            int updatedRows = statement.executeUpdate();
            if (updatedRows != 1) {
                throw new IllegalStateException("Refusing to repair migration " + inspection.scriptName()
                        + ": expected one guarded migration-history row but updated " + updatedRows + '.');
            }
        }

        LOGGER.info("Repaired line-ending-only checksum drift for applied migration {}", inspection.scriptName());
    }

    private static MigrationFile readMigrationFile(Path migrationFile) throws IOException {
        String scriptName = migrationFile.getFileName().toString();
        Matcher matcher = MIGRATION_FILE_PATTERN.matcher(scriptName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid versioned migration file name: " + scriptName);
        }

        return new MigrationFile(Integer.parseInt(matcher.group(1)), scriptName,
                Files.readString(migrationFile, StandardCharsets.UTF_8));
    }

    private static String normalizeLineEndings(String content, String lineSeparator) {
        return content.replace("\r\n", "\n").replace("\r", "\n").replace("\n", lineSeparator);
    }

    private static String checksum(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 checksum algorithm is not available.", e);
        }
    }

    /**
     * Immutable migration checksum comparison that callers inspect before opting into a repair.
     *
     * @param version migration version
     * @param scriptName versioned migration file name
     * @param recordedChecksum checksum currently stored in {@code schema_migrations}
     * @param currentChecksum checksum of the exact current migration file text
     * @param lfNormalizedChecksum checksum after normalizing all line endings to LF
     * @param crLfNormalizedChecksum checksum after normalizing all line endings to CRLF
     */
    public record ChecksumInspection(int version, String scriptName, String recordedChecksum, String currentChecksum,
                                     String lfNormalizedChecksum, String crLfNormalizedChecksum) {

        /**
         * Returns whether the recorded checksum differs from the current file but matches a line-ending variant.
         *
         * @return {@code true} only when a guarded repair is safe to offer
         */
        public boolean isLineEndingOnlyMismatch() {
            return recordedChecksum != null && !recordedChecksum.isBlank()
                    && !recordedChecksum.equals(currentChecksum)
                    && (recordedChecksum.equals(lfNormalizedChecksum)
                    || recordedChecksum.equals(crLfNormalizedChecksum));
        }
    }

    private record MigrationFile(int version, String scriptName, String content) {
    }
}
