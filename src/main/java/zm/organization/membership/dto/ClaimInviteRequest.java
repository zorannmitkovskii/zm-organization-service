package zm.organization.membership.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimInviteRequest(

        @NotBlank
        @Size(max = 32)
        String code,

        @NotBlank
        @Size(max = 64)
        String realm,

        @NotBlank
        @Size(max = 64)
        String userId
) {
}
