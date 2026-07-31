package zm.organization.common;

import java.util.Map;

/**
 * Folds Macedonian Cyrillic to lowercase Latin so that trigram search matches
 * across scripts.
 *
 * <p>Without this, "panorama skopje" and "панорама скопје" share no trigrams at
 * all and pg_trgm scores them zero — yet they are the same restaurant, and a
 * user onboarding from a phone keyboard may type either. The transliterated
 * form is stored alongside the display name and is what the index covers; the
 * query is transliterated the same way, so both scripts converge.
 *
 * <p>Digraphs matter: ж→zh, ч→ch, ш→sh, џ→dj, ѓ→gj, ќ→kj, љ→lj, њ→nj, ѕ→dz.
 * Mapping them to single letters would collide (ч and ц both to "c"), which
 * costs precision on exactly the names that need it.
 */
public final class Transliterator {

    private static final Map<Character, String> CYRILLIC_TO_LATIN = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"),
            Map.entry('г', "g"), Map.entry('д', "d"), Map.entry('ѓ', "gj"),
            Map.entry('е', "e"), Map.entry('ж', "zh"), Map.entry('з', "z"),
            Map.entry('ѕ', "dz"), Map.entry('и', "i"), Map.entry('ј', "j"),
            Map.entry('к', "k"), Map.entry('л', "l"), Map.entry('љ', "lj"),
            Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('њ', "nj"),
            Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"),
            Map.entry('с', "s"), Map.entry('т', "t"), Map.entry('ќ', "kj"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "h"),
            Map.entry('ц', "c"), Map.entry('ч', "ch"), Map.entry('џ', "dj"),
            Map.entry('ш', "sh"));

    private Transliterator() {
    }

    /**
     * Returns a lowercase Latin form with runs of non-alphanumerics collapsed
     * to single spaces. Returns null for null so callers can pass optional
     * fields straight through.
     */
    public static String toSearchForm(String value) {
        if (value == null) {
            return null;
        }

        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toLowerCase().toCharArray()) {
            String latin = CYRILLIC_TO_LATIN.get(c);
            if (latin != null) {
                out.append(latin);
            } else if (Character.isLetterOrDigit(c)) {
                out.append(c);
            } else {
                out.append(' ');
            }
        }

        return out.toString().replaceAll("\\s+", " ").trim();
    }
}
