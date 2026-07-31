package zm.organization.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import zm.organization.common.validation.ValidTaxId;

/**
 * Status is absent on purpose: a new organization is always ACTIVE.
 * Suspension happens through DELETE.
 */
public record CreateOrganizationRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 255)
        String legalName,

        @ValidTaxId
        String taxId,

        @Size(max = 32)
        String vatNumber,

        @Email
        @Size(max = 255)
        String contactEmail,

        @Pattern(regexp = "^\\+?[0-9 ()-]{6,20}$", message = "must be a valid phone number")
        String contactPhone,

        @Size(max = 255)
        String website,

        @Size(max = 512)
        String logoKey,

        @Size(max = 8)
        String defaultLang,

        @NotBlank
        @Size(max = 64)
        String createdByApp
) {
}
