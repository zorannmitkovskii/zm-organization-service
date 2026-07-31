package zm.organization.membership;

/**
 * What a member may do — interpreted by the products, not by this service.
 *
 * <p>This service only answers "what role does this user hold here". Whether
 * an ADMIN may edit a particular menu is a decision for the product that owns
 * the menu.
 */
public enum MemberRole {
    OWNER,
    ADMIN,
    MEMBER
}
