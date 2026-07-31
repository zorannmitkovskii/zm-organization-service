package zm.organization.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateLocationRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 255)
        String address,

        @Size(max = 120)
        String city,

        @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO 3166-1 alpha-2 country code")
        String country,

        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        BigDecimal geoLat,

        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        BigDecimal geoLng,

        @Pattern(regexp = "^\\+?[0-9 ()-]{6,20}$", message = "must be a valid phone number")
        String contactPhone,

        Map<String, String> workingHours,

        Boolean active
) {
}
