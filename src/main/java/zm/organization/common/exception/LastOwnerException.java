package zm.organization.common.exception;

/**
 * An organization must always have at least one owner. Without one, nobody can
 * issue invites or promote anyone — an unrecoverable state that would need a
 * database edit to escape.
 */
public class LastOwnerException extends RuntimeException {

    public LastOwnerException(String action) {
        super("Cannot " + action + " the last owner of the organization");
    }
}
