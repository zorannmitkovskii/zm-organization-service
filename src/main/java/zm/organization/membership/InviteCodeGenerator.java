package zm.organization.membership;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generates and hashes invite codes.
 *
 * <p>The alphabet omits characters people confuse when reading a code off a
 * screen and typing it elsewhere: {@code 0/O}, {@code 1/I/L}, {@code 8/B}.
 * Codes get spoken over the phone between a restaurant owner and a colleague,
 * so that matters more than the two extra bits.
 */
@Component
public class InviteCodeGenerator {

    static final String ALPHABET = "ACDEFGHJKMNPQRTUVWXY2345679";
    static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /**
     * SHA-256, hex encoded. Deterministic on purpose — claiming looks the
     * invite up by its code, which a salted hash would make impossible.
     */
    public String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(code.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }
}
