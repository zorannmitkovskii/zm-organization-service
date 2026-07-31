package zm.organization.organization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import zm.organization.organization.dto.CreateOrganizationRequest;
import zm.organization.organization.dto.UpdateOrganizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The partial-update contract is the part worth pinning down: a PATCH must
 * leave every field the caller did not send exactly as it was.
 */
class OrganizationMapperTest {

    private final OrganizationMapper mapper = Mappers.getMapper(OrganizationMapper.class);

    private static Organization existing() {
        Organization entity = new Organization();
        entity.setName("Ресторан Панорама");
        entity.setLegalName("Панорама ДООЕЛ Скопје");
        entity.setTaxId("4080012345678");
        entity.setContactEmail("kontakt@panorama.mk");
        entity.setContactPhone("+38970123456");
        entity.setDefaultLang("mk");
        entity.setStatus(OrganizationStatus.ACTIVE);
        entity.setCreatedByApp("menu-app");
        return entity;
    }

    @Test
    @DisplayName("Create maps the request and leaves status to the service")
    void createMapsRequest() {
        var request = new CreateOrganizationRequest(
                "Кафе Уно", "Уно ДООЕЛ", "4080087654321", "MK4080087654321",
                "info@kafeuno.mk", "+38971222333", "https://kafeuno.mk",
                "logos/uno.png", "mk", "menu-app");

        Organization entity = mapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo("Кафе Уно");
        assertThat(entity.getTaxId()).isEqualTo("4080087654321");
        assertThat(entity.getCreatedByApp()).isEqualTo("menu-app");
        assertThat(entity.getStatus()).isNull();
    }

    @Test
    @DisplayName("PATCH with one field leaves the others untouched")
    void partialUpdateTouchesOnlySentFields() {
        Organization entity = existing();

        mapper.applyUpdate(entity, new UpdateOrganizationRequest(
                null, null, null, null, "+38971999888", null, null, null));

        assertThat(entity.getContactPhone()).isEqualTo("+38971999888");
        assertThat(entity.getName()).isEqualTo("Ресторан Панорама");
        assertThat(entity.getLegalName()).isEqualTo("Панорама ДООЕЛ Скопје");
        assertThat(entity.getContactEmail()).isEqualTo("kontakt@panorama.mk");
        assertThat(entity.getDefaultLang()).isEqualTo("mk");
    }

    @Test
    @DisplayName("PATCH cannot change tax id, status or the creating app")
    void immutableFieldsSurviveUpdate() {
        Organization entity = existing();

        mapper.applyUpdate(entity, new UpdateOrganizationRequest(
                "Ново име", null, null, null, null, null, null, null));

        assertThat(entity.getName()).isEqualTo("Ново име");
        assertThat(entity.getTaxId()).isEqualTo("4080012345678");
        assertThat(entity.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(entity.getCreatedByApp()).isEqualTo("menu-app");
    }
}
