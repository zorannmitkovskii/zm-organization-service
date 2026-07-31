package zm.organization.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.organization.audit.AuditService;
import zm.organization.audit.AuditTargetType;
import zm.organization.common.ChangedFields;
import zm.organization.common.Transliterator;
import zm.organization.common.exception.ResourceNotFoundException;
import zm.organization.common.exception.TaxIdAlreadyExistsException;
import zm.organization.organization.dto.CreateOrganizationRequest;
import zm.organization.organization.dto.OrganizationResponse;
import zm.organization.organization.dto.UpdateOrganizationRequest;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final String DEFAULT_LANG = "mk";

    private final OrganizationRepository repository;
    private final OrganizationMapper mapper;
    private final AuditService audit;

    /**
     * Deliberately not {@code @Transactional}. The insert has to be able to
     * fail on the unique index and then be followed by a read of the row that
     * won — and a constraint violation marks the surrounding transaction
     * rollback-only, so that read would fail too. Letting each repository call
     * run in its own transaction keeps the recovery path usable. Nothing here
     * needs multi-statement atomicity: it is one insert.
     */
    public OrganizationResponse create(CreateOrganizationRequest request) {
        rejectIfTaxIdTaken(request.taxId());

        Organization entity = mapper.toEntity(request);
        entity.setStatus(OrganizationStatus.ACTIVE);
        entity.setSearchName(Transliterator.toSearchForm(entity.getName()));
        if (entity.getDefaultLang() == null || entity.getDefaultLang().isBlank()) {
            entity.setDefaultLang(DEFAULT_LANG);
        }

        Organization saved = save(entity, request.taxId());
        audit.record("CREATE_ORGANIZATION", AuditTargetType.ORG, saved.getId(),
                List.of("name", "taxId", "createdByApp"));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse findById(UUID id) {
        return mapper.toResponse(requireOrganization(id));
    }

    @Transactional
    public OrganizationResponse update(UUID id, UpdateOrganizationRequest request) {
        Organization entity = requireOrganization(id);
        mapper.applyUpdate(entity, request);
        entity.setSearchName(Transliterator.toSearchForm(entity.getName()));
        Organization saved = repository.save(entity);
        audit.record("UPDATE_ORGANIZATION", AuditTargetType.ORG, id, ChangedFields.of(request));
        return mapper.toResponse(saved);
    }

    /**
     * DELETE suspends rather than removes. Products hold {@code orgId} as a
     * foreign reference; deleting the row would break them, and a suspended
     * organization is still readable so each product can decide what that
     * means for its own data.
     */
    @Transactional
    public void suspend(UUID id) {
        Organization entity = requireOrganization(id);
        entity.setStatus(OrganizationStatus.SUSPENDED);
        repository.save(entity);
        audit.record("SUSPEND_ORGANIZATION", AuditTargetType.ORG, id, List.of("status"));
    }

    /**
     * Returns the entity or throws — the 404 path for anything that hangs off
     * an organization. {@code LocationService} needs the entity itself to set
     * the association, which is why this is not the DTO-returning lookup.
     */
    public Organization requireOrganization(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
    }

    private void rejectIfTaxIdTaken(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            return;
        }
        repository.findByTaxId(taxId).ifPresent(existing -> {
            throw new TaxIdAlreadyExistsException(taxId, existing.getId());
        });
    }

    /**
     * The pre-check above is a courtesy: it produces a clean 409 in the common
     * case. Two concurrent creates can both pass it, so the partial unique
     * index is the actual guarantee — exactly one insert survives, and the
     * loser is translated into the same 409 here.
     */
    private Organization save(Organization entity, String taxId) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            UUID winner = repository.findByTaxId(taxId).map(Organization::getId).orElse(null);
            if (winner == null) {
                throw e;
            }
            throw new TaxIdAlreadyExistsException(taxId, winner);
        }
    }
}
