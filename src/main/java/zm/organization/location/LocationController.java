package zm.organization.location;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zm.organization.location.dto.CreateLocationRequest;
import zm.organization.location.dto.LocationResponse;
import zm.organization.location.dto.UpdateLocationRequest;

import java.util.List;
import java.util.UUID;

/**
 * Split across two base paths on purpose: creating and listing are scoped to
 * an organization, while updating and deleting address a location directly and
 * do not need the parent in the URL.
 */
@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService service;

    @PostMapping("/internal/organizations/{orgId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse create(@PathVariable UUID orgId,
                                   @Valid @RequestBody CreateLocationRequest request) {
        return service.create(orgId, request);
    }

    @GetMapping("/internal/organizations/{orgId}/locations")
    public List<LocationResponse> listByOrganization(@PathVariable UUID orgId) {
        return service.findByOrganization(orgId);
    }

    @PatchMapping("/internal/locations/{id}")
    public LocationResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateLocationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/internal/locations/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
