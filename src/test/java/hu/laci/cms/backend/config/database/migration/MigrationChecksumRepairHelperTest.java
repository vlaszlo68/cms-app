package hu.laci.cms.backend.config.database.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Tests for guarded, line-ending-only migration checksum repair support. */
class MigrationChecksumRepairHelperTest {

    @Test
    void identifiesCrLfOnlyMismatchAndRepairsWithRecordedChecksumGuard(@TempDir Path temporaryDirectory)
            throws Exception {
        Path migrationFile = temporaryDirectory.resolve("V17__example.sql");
        Files.writeString(migrationFile, "CREATE TABLE example (id BIGINT);\r\n", StandardCharsets.UTF_8);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement selectStatement = Mockito.mock(PreparedStatement.class);
        ResultSet rows = Mockito.mock(ResultSet.class);
        PreparedStatement updateStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(Mockito.startsWith("SELECT script_name, checksum")))
                .thenReturn(selectStatement);
        Mockito.when(selectStatement.executeQuery()).thenReturn(rows);
        Mockito.when(rows.next()).thenReturn(true);
        Mockito.when(rows.getString("script_name")).thenReturn("V17__example.sql");
        Mockito.when(rows.getString("checksum"))
                .thenReturn("d2fee209b7f19c38becd7c8c936a3eb09c0e810de24be9b9d929247de11f3021");
        Mockito.when(connection.prepareStatement(Mockito.startsWith("UPDATE schema_migrations")))
                .thenReturn(updateStatement);
        Mockito.when(updateStatement.executeUpdate()).thenReturn(1);

        MigrationChecksumRepairHelper.ChecksumInspection inspection =
                MigrationChecksumRepairHelper.inspect(connection, migrationFile);

        Assertions.assertTrue(inspection.isLineEndingOnlyMismatch());

        MigrationChecksumRepairHelper.repairLineEndingOnlyMismatch(connection, inspection);

        ArgumentCaptor<String> checksums = ArgumentCaptor.forClass(String.class);
        Mockito.verify(updateStatement, Mockito.times(3)).setString(Mockito.anyInt(), checksums.capture());
        Assertions.assertEquals(inspection.currentChecksum(), checksums.getAllValues().get(0));
        Assertions.assertEquals(inspection.scriptName(), checksums.getAllValues().get(1));
        Assertions.assertEquals(inspection.recordedChecksum(), checksums.getAllValues().get(2));
    }

    @Test
    void rejectsChecksumMismatchThatIsNotExplainedByLineEndings(@TempDir Path temporaryDirectory)
            throws Exception {
        Path migrationFile = temporaryDirectory.resolve("V17__example.sql");
        Files.writeString(migrationFile, "CREATE TABLE changed_example (id BIGINT);\r\n", StandardCharsets.UTF_8);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement selectStatement = Mockito.mock(PreparedStatement.class);
        ResultSet rows = Mockito.mock(ResultSet.class);
        Mockito.when(connection.prepareStatement(Mockito.startsWith("SELECT script_name, checksum")))
                .thenReturn(selectStatement);
        Mockito.when(selectStatement.executeQuery()).thenReturn(rows);
        Mockito.when(rows.next()).thenReturn(true);
        Mockito.when(rows.getString("script_name")).thenReturn("V17__example.sql");
        Mockito.when(rows.getString("checksum")).thenReturn("not-a-matching-checksum");

        MigrationChecksumRepairHelper.ChecksumInspection inspection =
                MigrationChecksumRepairHelper.inspect(connection, migrationFile);

        Assertions.assertFalse(inspection.isLineEndingOnlyMismatch());
        Assertions.assertThrows(IllegalStateException.class,
                () -> MigrationChecksumRepairHelper.repairLineEndingOnlyMismatch(connection, inspection));
    }
}
