package zm.organization.organization;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zm.organization.organization.dto.CreateOrganizationRequest;
import zm.organization.organization.dto.OrganizationResponse;
import zm.organization.organization.dto.UpdateOrganizationRequest;

import java.util.UUID;

/**
 * Machine-to-machine API. Authentication arrives in ORG-04 — until then these
 * endpoints are open, which is why the service is not deployed anywhere that
 * matters yet.
 *
 * <p>There is deliberately no "list all organizations" endpoint. Enumerating
 * the registry is a data-leak surface; ORG-05 adds search with masking and
 * rate limiting instead.
 */
@RestController
@RequestMapping("/internal/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public OrganizationResponse getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}")
    public OrganizationResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateOrganizationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> suspend(@PathVariable UUID id) {
        service.suspend(id);
        return ResponseEntity.noContent().build();
    }
}
