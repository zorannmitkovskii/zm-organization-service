package zm.organization.common.exception;

/**
 * The invite exists but is spent — expired or out of uses. Maps to 410 Gone
 * so the caller can tell "this code was real but is finished" apart from
 * "no such code" (404) and show a useful message.
 */
public class InviteNotUsableException extends RuntimeException {

    public InviteNotUsableException() {
        super("This invite code has expired or has already been used");
    }
}
