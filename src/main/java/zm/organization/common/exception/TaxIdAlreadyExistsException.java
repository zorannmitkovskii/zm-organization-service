package zm.organization.common.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Carries the id of the organization that already holds the tax id. That id is
 * the whole point of the 409: the calling product shows "this company already
 * exists — ask its owner for an invite code" instead of creating a duplicate.
 */
@Getter
public class TaxIdAlreadyExistsException extends RuntimeException {

    private final UUID existingOrgId;

    public TaxIdAlreadyExistsException(String taxId, UUID existingOrgId) {
        super("An organization with tax id " + taxId + " already exists");
        this.existingOrgId = existingOrgId;
    }
}
