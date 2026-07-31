package zm.organization.organization;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import zm.organization.organization.dto.CreateOrganizationRequest;
import zm.organization.organization.dto.OrganizationResponse;
import zm.organization.organization.dto.UpdateOrganizationRequest;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    // Derived from name by the service; see Transliterator.
    @Mapping(target = "searchName", ignore = true)
    Organization toEntity(CreateOrganizationRequest request);

    OrganizationResponse toResponse(Organization entity);

    /**
     * Partial update: IGNORE leaves the entity field untouched when the request
     * field is null, which is exactly PATCH semantics.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "taxId", ignore = true)
    @Mapping(target = "createdByApp", ignore = true)
    @Mapping(target = "searchName", ignore = true)
    void applyUpdate(@MappingTarget Organization entity, UpdateOrganizationRequest request);
}
