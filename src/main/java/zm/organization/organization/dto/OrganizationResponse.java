package zm.organization.organization.dto;

import zm.organization.organization.OrganizationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String legalName,
        String taxId,
        String vatNumber,
        String contactEmail,
        String contactPhone,
        String website,
        String logoKey,
        String defaultLang,
        OrganizationStatus status,
        String createdByApp,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
