package zm.organization.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TransliteratorTest {

    @ParameterizedTest
    @CsvSource({
            "Ресторан Панорама, restoran panorama",
            "Кафе Уно Скопје, kafe uno skopje",
            "Чаршија, charshija",
            "Џумаја, djumaja",
            "Ѓорче Петров, gjorche petrov",
            "Љубојно, ljubojno",
            "Њујорк, njujork",
            "Ѕвезда, dzvezda",
            "Ќерамидница, kjeramidnica",
            "Шише Жито, shishe zhito"
    })
    @DisplayName("Macedonian Cyrillic folds to the expected Latin form")
    void transliteratesCyrillic(String input, String expected) {
        assertThat(Transliterator.toSearchForm(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Latin input is only lowercased — it is already in the target script")
    void latinPassesThrough() {
        assertThat(Transliterator.toSearchForm("Panorama Skopje")).isEqualTo("panorama skopje");
    }

    @Test
    @DisplayName("Both scripts of the same name converge on one form — the point of the whole thing")
    void bothScriptsConverge() {
        assertThat(Transliterator.toSearchForm("Панорама"))
                .isEqualTo(Transliterator.toSearchForm("panorama"));
    }

    @Test
    @DisplayName("Digraph letters stay distinct so ч and ц do not collide")
    void digraphsDoNotCollide() {
        assertThat(Transliterator.toSearchForm("Чачак"))
                .isNotEqualTo(Transliterator.toSearchForm("Цацак"));
    }

    @Test
    @DisplayName("Punctuation and repeated whitespace collapse to single spaces")
    void punctuationCollapses() {
        assertThat(Transliterator.toSearchForm("  Ресторан   \"Панорама\" - ДООЕЛ  "))
                .isEqualTo("restoran panorama dooel");
    }

    @Test
    @DisplayName("Digits survive — they are part of many business names")
    void digitsSurvive() {
        assertThat(Transliterator.toSearchForm("Кафе 13")).isEqualTo("kafe 13");
    }

    @Test
    @DisplayName("Null passes through so optional fields need no guard at the call site")
    void nullPassesThrough() {
        assertThat(Transliterator.toSearchForm(null)).isNull();
    }
}
