package zm.organization.location;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.organization.audit.AuditService;
import zm.organization.audit.AuditTargetType;
import zm.organization.common.ChangedFields;
import zm.organization.common.Transliterator;
import zm.organization.common.exception.ResourceNotFoundException;
import zm.organization.location.dto.CreateLocationRequest;
import zm.organization.location.dto.LocationResponse;
import zm.organization.location.dto.UpdateLocationRequest;
import zm.organization.organization.Organization;
import zm.organization.organization.OrganizationService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository repository;
    private final LocationMapper mapper;
    private final OrganizationService organizationService;
    private final AuditService audit;

    @Transactional
    public LocationResponse create(UUID orgId, CreateLocationRequest request) {
        Organization organization = organizationService.requireOrganization(orgId);

        Location entity = mapper.toEntity(request);
        entity.setOrganization(organization);
        entity.setActive(request.active() == null || request.active());
        entity.setSearchCity(Transliterator.toSearchForm(entity.getCity()));

        Location saved = repository.save(entity);
        audit.record("CREATE_LOCATION", AuditTargetType.LOCATION, saved.getId(),
                List.of("name", "orgId"));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> findByOrganization(UUID orgId) {
        organizationService.requireOrganization(orgId);
        return repository.findByOrganizationIdOrderByNameAsc(orgId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public LocationResponse update(UUID id, UpdateLocationRequest request) {
        Location entity = require(id);
        mapper.applyUpdate(entity, request);
        entity.setSearchCity(Transliterator.toSearchForm(entity.getCity()));
        Location saved = repository.save(entity);
        audit.record("UPDATE_LOCATION", AuditTargetType.LOCATION, id, ChangedFields.of(request));
        return mapper.toResponse(saved);
    }

    /**
     * Locations are removed outright, unlike organizations. Nothing outside
     * this service references a location id yet, so there is no reference to
     * protect — and a closed branch that lingers in every product's UI is
     * worse than one that disappears.
     */
    @Transactional
    public void delete(UUID id) {
        repository.delete(require(id));
        audit.record("DELETE_LOCATION", AuditTargetType.LOCATION, id, List.of());
    }

    private Location require(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", id));
    }
}
