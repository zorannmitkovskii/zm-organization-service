package zm.organization.membership.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import zm.organization.membership.MemberRole;

public record AddMemberRequest(

        @NotBlank
        @Size(max = 64)
        String realm,

        @NotBlank
        @Size(max = 64)
        String userId,

        @NotNull
        MemberRole role
) {
}
