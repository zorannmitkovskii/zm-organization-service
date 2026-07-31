package zm.organization.location;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import zm.organization.location.dto.CreateLocationRequest;
import zm.organization.location.dto.LocationResponse;
import zm.organization.location.dto.UpdateLocationRequest;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "active", ignore = true)
    // Derived from city by the service; see Transliterator.
    @Mapping(target = "searchCity", ignore = true)
    Location toEntity(CreateLocationRequest request);

    @Mapping(target = "orgId", source = "organization.id")
    LocationResponse toResponse(Location entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "searchCity", ignore = true)
    void applyUpdate(@MappingTarget Location entity, UpdateLocationRequest request);
}
