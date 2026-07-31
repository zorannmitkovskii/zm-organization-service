package zm.organization.search;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The endpoint every product's onboarding calls before creating an
 * organization, so that a restaurant already in the registry as an Ivy vendor
 * is offered "ask for access" instead of quietly becoming a duplicate.
 *
 * <p>{@code realm} is what makes the answer actionable: it tells the caller
 * whether this organization already has members in the caller's own realm, and
 * therefore whether to show "sign in" or "ask the owner for an invite code".
 */
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;

    @GetMapping("/internal/organizations/search")
    public List<OrganizationSummary> search(
            @RequestParam(required = false) String taxId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false, defaultValue = "") String realm) {

        if (taxId != null && !taxId.isBlank()) {
            return service.byTaxId(taxId.trim(), realm);
        }
        if (name != null && !name.isBlank()) {
            return service.byName(name, city, realm);
        }
        return List.of();
    }
}
