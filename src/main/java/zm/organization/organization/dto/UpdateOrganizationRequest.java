package zm.organization.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH payload: every field is optional and {@code null} means "leave alone".
 *
 * <p>The consequence is that a field cannot be cleared through this endpoint —
 * sending {@code null} is indistinguishable from omitting the key. That is the
 * accepted trade-off for a plain record DTO; a caller that must clear a value
 * sends an empty string.
 *
 * <p>Three fields are deliberately absent. {@code taxId} is the deduplication
 * key products have already resolved against, so it is fixed at creation;
 * {@code status} changes only through DELETE; {@code createdByApp} is history.
 */
public record UpdateOrganizationRequest(

        @Size(max = 200)
        String name,

        @Size(max = 255)
        String legalName,

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
        String defaultLang
) {
}
