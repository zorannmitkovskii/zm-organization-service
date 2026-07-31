package zm.organization.membership.dto;

import zm.organization.membership.MemberRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code code} is the plaintext and is present only in the response to the
 * request that created the invite — the database keeps a hash. If the caller
 * loses it, they issue a new invite.
 */
public record InviteResponse(
        UUID id,
        UUID orgId,
        String code,
        MemberRole role,
        LocalDateTime expiresAt,
        int maxUses,
        int useCount
) {
}
