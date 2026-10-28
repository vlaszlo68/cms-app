package hu.laci.cms.backend.config.database.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests the formatting-insensitive representation used for migration checksums. */
class MigrationChecksumCanonicalizerTest {

    @Test
    void normalizesLineEndingsTrailingWhitespaceAndTrailingBlankLines() {
        String canonical = MigrationChecksumCanonicalizer.canonicalize(
                "CREATE TABLE example (id BIGINT);  \r\n\r\n\t\r\n");

        Assertions.assertEquals("CREATE TABLE example (id BIGINT);", canonical);
    }

    @Test
    void preservesWhitespaceInsideSingleQuotedAndDollarQuotedValues() {
        String canonical = MigrationChecksumCanonicalizer.canonicalize(
                "INSERT INTO example VALUES ('kept  \r\n');  \r\n"
                        + "DO $$BEGIN\r\nPERFORM 'kept  \r\n';\r\nEND$$;   \r\n");

        Assertions.assertEquals("INSERT INTO example VALUES ('kept  \n');\n"
                        + "DO $$BEGIN\nPERFORM 'kept  \n';\nEND$$;", canonical);
    }
}
