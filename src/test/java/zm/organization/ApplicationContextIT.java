package zm.organization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** ORG-01: the context loads and Flyway has applied the baseline migration. */
class ApplicationContextIT extends AbstractPostgresIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway applies V1 baseline and records it in the schema history")
    void flywayAppliesBaseline() {
        Integer appliedV1 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class);

        assertThat(appliedV1).isEqualTo(1);
    }

    @Test
    @DisplayName("pg_trgm extension is available for the ORG-05 fuzzy search")
    void trigramExtensionInstalled() {
        Integer installed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'pg_trgm'", Integer.class);

        assertThat(installed).isEqualTo(1);
    }
}
