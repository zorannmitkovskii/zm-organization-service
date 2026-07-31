package zm.organization.membership.dto;

import jakarta.validation.constraints.NotNull;
import zm.organization.membership.MemberRole;

/** Only the role is mutable — realm and userId identify the membership. */
public record UpdateMemberRequest(

        @NotNull
        MemberRole role
) {
}
