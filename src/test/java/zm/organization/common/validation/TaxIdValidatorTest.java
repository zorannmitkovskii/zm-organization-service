package zm.organization.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TaxIdValidatorTest {

    private static final String MK_EDB = "^[0-9]{13}$";

    private static boolean isValid(String pattern, String value) {
        return new TaxIdValidator(pattern).isValid(value, null);
    }

    @Test
    @DisplayName("A 13-digit Macedonian ЕДБ is accepted")
    void acceptsMacedonianTaxId() {
        assertThat(isValid(MK_EDB, "4080012345678")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "40800123456789", "408001234567a", "4080 0123 4567", "МК4080012345678"})
    @DisplayName("Wrong length or non-digits are rejected")
    void rejectsMalformedTaxIds(String value) {
        assertThat(isValid(MK_EDB, value)).isFalse();
    }

    @Test
    @DisplayName("Absent tax id is valid — a company may register before it has one")
    void acceptsNull() {
        assertThat(isValid(MK_EDB, null)).isTrue();
        assertThat(isValid(MK_EDB, "   ")).isTrue();
    }

    @Test
    @DisplayName("The pattern is configurable for other jurisdictions")
    void honoursConfiguredPattern() {
        String serbianPib = "^[0-9]{9}$";

        assertThat(isValid(serbianPib, "123456789")).isTrue();
        assertThat(isValid(serbianPib, "4080012345678")).isFalse();
    }
}
