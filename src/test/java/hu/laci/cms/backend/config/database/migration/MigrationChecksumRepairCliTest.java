package hu.laci.cms.backend.config.database.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/** Tests for the explicit migration checksum repair command-line entry point. */
class MigrationChecksumRepairCliTest {

    @Test
    void dryRunReportsInspectionAndRollsBackWithoutWriting(@TempDir Path temporaryDirectory) throws Exception {
        Path migrationFile = createCrLfMigration(temporaryDirectory);
        Connection connection = createConnectionWithLineEndingOnlyMismatch();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = MigrationChecksumRepairCli.run(new String[]{
                "--jdbc-url", "jdbc:postgresql://localhost:5432/cms_db",
                "--db-user", "operator",
                "--password-env", "TEST_DB_PASSWORD",
                "--migration", migrationFile.toString()
        }, Map.of("TEST_DB_PASSWORD", "secret"), (url, user, password) -> connection,
                new PrintStream(output), new PrintStream(new ByteArrayOutputStream()));

        Assertions.assertEquals(0, exitCode);
        Assertions.assertTrue(output.toString(StandardCharsets.UTF_8).contains("mode=DRY_RUN"));
        Mockito.verify(connection).rollback();
        Mockito.verify(connection, Mockito.never()).commit();
        Mockito.verify(connection, Mockito.never())
                .prepareStatement(Mockito.startsWith("UPDATE schema_migrations"));
    }

    @Test
    void confirmedRepairWritesOnlyAfterLineEndingInspection(@TempDir Path temporaryDirectory) throws Exception {
        Path migrationFile = createCrLfMigration(temporaryDirectory);
        Connection connection = createConnectionWithLineEndingOnlyMismatch();
        PreparedStatement updateStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(Mockito.startsWith("UPDATE schema_migrations")))
                .thenReturn(updateStatement);
        Mockito.when(updateStatement.executeUpdate()).thenReturn(1);

        int exitCode = MigrationChecksumRepairCli.run(new String[]{
                "--jdbc-url", "jdbc:postgresql://localhost:5432/cms_db",
                "--db-user", "operator",
                "--password-env", "TEST_DB_PASSWORD",
                "--migration", migrationFile.toString(),
                "--confirm-line-ending-repair"
        }, Map.of("TEST_DB_PASSWORD", "secret"), (url, user, password) -> connection,
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));

        Assertions.assertEquals(0, exitCode);
        Mockito.verify(connection).commit();
        Mockito.verify(connection, Mockito.never()).rollback();
    }

    @Test
    void rejectsMissingPasswordEnvironmentValue(@TempDir Path temporaryDirectory) throws Exception {
        Path migrationFile = createCrLfMigration(temporaryDirectory);
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int exitCode = MigrationChecksumRepairCli.run(new String[]{
                "--jdbc-url", "jdbc:postgresql://localhost:5432/cms_db",
                "--db-user", "operator",
                "--password-env", "TEST_DB_PASSWORD",
                "--migration", migrationFile.toString()
        }, Map.of(), (url, user, password) -> Assertions.fail("Connection must not be opened."),
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(errors));

        Assertions.assertEquals(2, exitCode);
        Assertions.assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Password environment variable"));
    }

    private Path createCrLfMigration(Path temporaryDirectory) throws Exception {
        Path migrationFile = temporaryDirectory.resolve("V17__example.sql");
        Files.writeString(migrationFile, "CREATE TABLE example (id BIGINT);\r\n", StandardCharsets.UTF_8);
        return migrationFile;
    }

    private Connection createConnectionWithLineEndingOnlyMismatch() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement selectStatement = Mockito.mock(PreparedStatement.class);
        ResultSet rows = Mockito.mock(ResultSet.class);
        Mockito.when(connection.prepareStatement(Mockito.startsWith("SELECT script_name, checksum")))
                .thenReturn(selectStatement);
        Mockito.when(selectStatement.executeQuery()).thenReturn(rows);
        Mockito.when(rows.next()).thenReturn(true);
        Mockito.when(rows.getString("script_name")).thenReturn("V17__example.sql");
        Mockito.when(rows.getString("checksum"))
                .thenReturn("d2fee209b7f19c38becd7c8c936a3eb09c0e810de24be9b9d929247de11f3021");
        return connection;
    }
}
