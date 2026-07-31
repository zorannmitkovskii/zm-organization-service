package zm.organization.organization;

/**
 * Lifecycle of an organization.
 *
 * <p>There is no DELETED state on purpose: products hold {@code orgId} as a
 * foreign reference, so removing the row would break them. DELETE suspends,
 * and each product decides what a suspended organization means for it.
 */
public enum OrganizationStatus {
    ACTIVE,
    SUSPENDED
}
