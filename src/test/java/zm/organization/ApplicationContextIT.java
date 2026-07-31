package zm.organization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** ORG-01: the context loads and Flyway has applied every migration. */
class ApplicationContextIT extends AbstractPostgresIT {

    @Test
    @DisplayName("Flyway applies every migration and records them as successful")
    void flywayAppliesAllMigrations() {
        Integer failed = jdbc().queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        Integer applied = jdbc().queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);

        assertThat(failed).isZero();
        assertThat(applied).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("pg_trgm extension is available for the ORG-05 fuzzy search")
    void trigramExtensionInstalled() {
        Integer installed = jdbc().queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'pg_trgm'", Integer.class);

        assertThat(installed).isEqualTo(1);
    }
}
