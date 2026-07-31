package zm.organization.membership.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import zm.organization.membership.MemberRole;

import java.util.UUID;

public record CreateInviteRequest(

        @NotNull
        MemberRole role,

        /** Defaults to the configured TTL when absent. */
        @Min(1)
        @Max(90)
        Integer ttlDays,

        /** Defaults to 1 — a code for one specific person. */
        @Min(1)
        @Max(100)
        Integer maxUses,

        /** The member issuing the invite, for the audit trail. Optional. */
        UUID createdByMember
) {
}
