package zm.organization.search;

import java.util.UUID;

/**
 * What a product may see about an organization it does not yet belong to.
 *
 * <p>Enough to recognise the company — "yes, that is us" — and nothing more.
 * Contact details are masked because search is reachable by every backend on
 * the platform and an unmasked one would be a convenient way to harvest
 * business email addresses.
 */
public record OrganizationSummary(
        UUID orgId,
        String name,
        String city,
        String contactEmail,
        String contactPhone,
        boolean hasMembersInRealm,
        Double score
) {
}
