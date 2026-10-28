package hu.laci.cms.backend.config.database.migration;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Explicit operator command for inspecting or repairing line-ending-only migration checksum drift.
 * <p>
 * The command is dry-run by default. It never repairs a checksum unless the caller supplies
 * {@code --confirm-line-ending-repair} and {@link MigrationChecksumRepairHelper} proves that the
 * recorded checksum matches an LF or CRLF normalization of the current migration file.
 */
public final class MigrationChecksumRepairCli {

    private static final String CONFIRM_REPAIR_OPTION = "--confirm-line-ending-repair";

    private MigrationChecksumRepairCli() {
    }

    /**
     * Runs the checksum inspection command.
     *
     * @param arguments command-line arguments; use {@code --help} for supported options
     */
    public static void main(String[] arguments) {
        int exitCode = run(arguments, System.getenv(), DriverManager::getConnection, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Executes the command with supplied infrastructure for testability.
     *
     * @param arguments command-line arguments
     * @param environment environment values used to obtain the password without placing it on the command line
     * @param connectionFactory factory for opening the explicit target database connection
     * @param output normal command output
     * @param errorOutput validation and execution error output
     * @return {@code 0} on a successful inspection or repair, {@code 1} on execution failure, or {@code 2}
     *         for invalid command usage
     */
    static int run(String[] arguments, Map<String, String> environment, ConnectionFactory connectionFactory,
                   PrintStream output, PrintStream errorOutput) {
        Command command;
        try {
            command = parseCommand(arguments);
        } catch (IllegalArgumentException e) {
            errorOutput.println(e.getMessage());
            printUsage(errorOutput);
            return 2;
        }

        if (command.help()) {
            printUsage(output);
            return 0;
        }

        String password = environment.get(command.passwordEnvironmentVariable());
        if (password == null || password.isBlank()) {
            errorOutput.println("Password environment variable " + command.passwordEnvironmentVariable()
                    + " is missing or blank.");
            return 2;
        }

        try (Connection connection = connectionFactory.open(command.jdbcUrl(), command.databaseUser(), password)) {
            connection.setAutoCommit(false);
            try {
                MigrationChecksumRepairHelper.ChecksumInspection inspection =
                        MigrationChecksumRepairHelper.inspect(connection, command.migrationFile());
                printInspection(output, inspection);

                if (command.confirmRepair()) {
                    MigrationChecksumRepairHelper.repairLineEndingOnlyMismatch(connection, inspection);
                    connection.commit();
                    output.println("mode=REPAIRED");
                } else {
                    connection.rollback();
                    output.println("mode=DRY_RUN");
                }
                return 0;
            } catch (IOException | SQLException | IllegalStateException e) {
                rollbackQuietly(connection);
                errorOutput.println("Migration checksum command failed: " + e.getMessage());
                return 1;
            }
        } catch (SQLException e) {
            errorOutput.println("Migration checksum command failed: " + e.getMessage());
            return 1;
        }
    }

    private static Command parseCommand(String[] arguments) {
        Map<String, String> options = new HashMap<>();
        boolean confirmRepair = false;
        for (int index = 0; index < arguments.length; index++) {
            String argument = arguments[index];
            if ("--help".equals(argument)) {
                return Command.usageCommand();
            }
            if (CONFIRM_REPAIR_OPTION.equals(argument)) {
                confirmRepair = true;
                continue;
            }
            if (!argument.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + argument);
            }
            if (index + 1 >= arguments.length || arguments[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + argument);
            }
            if (!isValueOption(argument)) {
                throw new IllegalArgumentException("Unsupported option: " + argument);
            }
            if (options.put(argument, arguments[++index]) != null) {
                throw new IllegalArgumentException("Option specified more than once: " + argument);
            }
        }

        return new Command(requireOption(options, "--jdbc-url"), requireOption(options, "--db-user"),
                requireOption(options, "--password-env"), Path.of(requireOption(options, "--migration")),
                confirmRepair, false);
    }

    private static boolean isValueOption(String argument) {
        return "--jdbc-url".equals(argument) || "--db-user".equals(argument)
                || "--password-env".equals(argument) || "--migration".equals(argument);
    }

    private static String requireOption(Map<String, String> options, String option) {
        String value = options.get(option);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required option is missing: " + option);
        }
        return value;
    }

    private static void printInspection(PrintStream output,
                                        MigrationChecksumRepairHelper.ChecksumInspection inspection) {
        output.println("migration=" + inspection.scriptName());
        output.println("recordedChecksum=" + inspection.recordedChecksum());
        output.println("currentChecksum=" + inspection.currentChecksum());
        output.println("lfNormalizedChecksum=" + inspection.lfNormalizedChecksum());
        output.println("crLfNormalizedChecksum=" + inspection.crLfNormalizedChecksum());
        output.println("lineEndingOnlyMismatch=" + inspection.isLineEndingOnlyMismatch());
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original inspection or repair failure without reporting a second stack trace.
        }
    }

    private static void printUsage(PrintStream output) {
        output.println("Usage: MigrationChecksumRepairCli --jdbc-url <jdbc-url> --db-user <user>"
                + " --password-env <environment-variable> --migration <Vn__name.sql>"
                + " [--confirm-line-ending-repair]");
        output.println("Without --confirm-line-ending-repair, the command reports checksums and rolls back.");
    }

    /**
     * Opens the explicit database connection used by the operator command.
     */
    @FunctionalInterface
    interface ConnectionFactory {

        /**
         * Opens a JDBC connection for the supplied non-secret connection details.
         *
         * @param jdbcUrl explicit JDBC URL
         * @param databaseUser database login name
         * @param password password read from the selected environment variable
         * @return open JDBC connection
         * @throws SQLException if the connection cannot be opened
         */
        Connection open(String jdbcUrl, String databaseUser, String password) throws SQLException;
    }

    private record Command(String jdbcUrl, String databaseUser, String passwordEnvironmentVariable,
                           Path migrationFile, boolean confirmRepair, boolean help) {

        private static Command usageCommand() {
            return new Command(null, null, null, null, false, true);
        }
    }
}
