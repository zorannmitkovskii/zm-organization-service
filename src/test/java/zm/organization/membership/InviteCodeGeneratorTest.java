package zm.organization.membership;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InviteCodeGeneratorTest {

    private final InviteCodeGenerator generator = new InviteCodeGenerator();

    @Test
    @DisplayName("Codes are 8 characters from the unambiguous alphabet")
    void codeShapeIsStable() {
        String code = generator.generate();

        assertThat(code).hasSize(InviteCodeGenerator.CODE_LENGTH);
        assertThat(code.chars()).allMatch(c -> InviteCodeGenerator.ALPHABET.indexOf(c) >= 0);
    }

    @Test
    @DisplayName("The alphabet excludes characters people misread when typing a code")
    void alphabetOmitsConfusableCharacters() {
        assertThat(InviteCodeGenerator.ALPHABET)
                .doesNotContain("0").doesNotContain("O")
                .doesNotContain("1").doesNotContain("I").doesNotContain("L")
                .doesNotContain("8").doesNotContain("B");
    }

    @Test
    @DisplayName("Generated codes do not repeat over a large sample")
    void codesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            seen.add(generator.generate());
        }

        assertThat(seen).hasSize(5_000);
    }

    @Test
    @DisplayName("Hashing is deterministic — claiming looks the invite up by it")
    void hashIsDeterministic() {
        String code = generator.generate();

        assertThat(generator.hash(code)).isEqualTo(generator.hash(code));
        assertThat(generator.hash(code)).hasSize(64);
    }

    @Test
    @DisplayName("Hashing normalises case and surrounding whitespace")
    void hashNormalisesInput() {
        String canonical = generator.hash("A7K9M2QX");

        assertThat(generator.hash("a7k9m2qx")).isEqualTo(canonical);
        assertThat(generator.hash("  A7K9M2QX  ")).isEqualTo(canonical);
    }

    @Test
    @DisplayName("Different codes hash differently")
    void differentCodesDifferentHashes() {
        assertThat(generator.hash("A7K9M2QX")).isNotEqualTo(generator.hash("A7K9M2QY"));
    }
}
