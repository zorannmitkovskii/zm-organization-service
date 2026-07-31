package zm.organization.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactMaskerTest {

    @Test
    @DisplayName("An email keeps its first letter and domain, loses the rest")
    void masksEmailLocalPart() {
        assertThat(ContactMasker.maskEmail("kontakt@panorama.mk")).isEqualTo("k***@panorama.mk");
    }

    @Test
    @DisplayName("The masked email never contains the original local part")
    void maskedEmailLeaksNothing() {
        String masked = ContactMasker.maskEmail("racunovodstvo@firma.com.mk");

        assertThat(masked).doesNotContain("acunovodstvo");
        assertThat(masked).endsWith("@firma.com.mk");
    }

    @Test
    @DisplayName("A malformed email is reduced to nothing rather than passed through")
    void malformedEmailIsFullyMasked() {
        assertThat(ContactMasker.maskEmail("not-an-email")).isEqualTo("***");
        assertThat(ContactMasker.maskEmail("@leading")).isEqualTo("***");
    }

    @Test
    @DisplayName("A phone keeps the country prefix and last three digits")
    void masksPhoneMiddle() {
        String masked = ContactMasker.maskPhone("+38970123456");

        assertThat(masked).startsWith("+389").endsWith("456");
        assertThat(masked).doesNotContain("70123");
        assertThat(masked).hasSameSizeAs("+38970123456");
    }

    @Test
    @DisplayName("A short number is fully masked — there is nothing safe to keep")
    void shortPhoneIsFullyMasked() {
        assertThat(ContactMasker.maskPhone("12345")).isEqualTo("***");
    }

    @Test
    @DisplayName("Absent contact details stay absent rather than becoming asterisks")
    void nullStaysNull() {
        assertThat(ContactMasker.maskEmail(null)).isNull();
        assertThat(ContactMasker.maskPhone(null)).isNull();
        assertThat(ContactMasker.maskPhone("  ")).isNull();
    }
}
