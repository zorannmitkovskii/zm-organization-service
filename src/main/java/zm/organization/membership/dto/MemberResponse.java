package zm.organization.membership.dto;

import zm.organization.membership.MemberRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID orgId,
        String realm,
        String userId,
        MemberRole role,
        LocalDateTime createdAt
) {
}
