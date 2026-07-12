package hu.laci.cms.backend.config.database.migration;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Unit tests for migration-runner infrastructure failure handling. */
class DatabaseMigrationRunnerTest {

    @Test
    void wrapsDatabaseConnectionFailure() throws Exception {
        try (MockedStatic<DatabaseConfig> database = Mockito.mockStatic(DatabaseConfig.class)) {
            database.when(DatabaseConfig::getConnection).thenThrow(new java.sql.SQLException("unavailable"));

            IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                    DatabaseMigrationRunner::runMigrations);

            Assertions.assertTrue(exception.getMessage().contains("Failed to run database migrations"));
        }
    }
}
