package zm.organization.location.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        UUID orgId,
        String name,
        String address,
        String city,
        String country,
        BigDecimal geoLat,
        BigDecimal geoLng,
        String contactPhone,
        Map<String, String> workingHours,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
