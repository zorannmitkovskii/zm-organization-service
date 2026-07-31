package zm.organization.organization;

import zm.organization.common.BaseRepository;

import java.util.Optional;

public interface OrganizationRepository extends BaseRepository<Organization> {

    Optional<Organization> findByTaxId(String taxId);
}
