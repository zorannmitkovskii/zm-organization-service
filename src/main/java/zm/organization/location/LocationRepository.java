package zm.organization.location;

import zm.organization.common.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends BaseRepository<Location> {

    List<Location> findByOrganizationIdOrderByNameAsc(UUID organizationId);
}
