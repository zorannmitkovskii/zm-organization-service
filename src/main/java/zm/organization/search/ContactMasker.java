package zm.organization.search;

/**
 * Reduces contact details to a recognisable but useless form.
 *
 * <p>The goal is that the person onboarding can confirm "that is our office
 * address" while the value is worthless to anyone enumerating the registry.
 * The domain survives in an email because that is usually what makes a company
 * recognisable; the local part does not.
 */
final class ContactMasker {

    private ContactMasker() {
    }

    /** {@code kontakt@panorama.mk} → {@code k***@panorama.mk} */
    static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** {@code +38970123456} → {@code +389*****456} — country and last three. */
    static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 7) {
            return "***";
        }
        String head = trimmed.substring(0, 4);
        String tail = trimmed.substring(trimmed.length() - 3);
        return head + "*".repeat(trimmed.length() - 7) + tail;
    }
}
