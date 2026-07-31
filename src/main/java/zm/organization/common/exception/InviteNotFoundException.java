package zm.organization.common.exception;

/**
 * No invite matches the code.
 *
 * <p>The message is deliberately identical whatever the reason, so a caller
 * guessing codes learns nothing from the response about which guesses were
 * closer.
 */
public class InviteNotFoundException extends RuntimeException {

    public InviteNotFoundException() {
        super("No such invite code");
    }
}
