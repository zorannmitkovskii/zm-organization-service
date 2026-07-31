package zm.organization.search;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import zm.organization.common.Transliterator;
import zm.organization.common.exception.RateLimitExceededException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final String UNKNOWN_CALLER = "anonymous";

    private final OrganizationSearchRepository repository;
    private final SearchRateLimiter rateLimiter;

    @Value("${organization.search.similarity-threshold:0.3}")
    private double similarityThreshold;

    @Value("${organization.search.max-results:5}")
    private int maxResults;

    /**
     * The primary deduplication check. Exact match on the tax id — a company
     * either has this number or it does not, and a near miss is not a hint
     * worth giving.
     */
    public List<OrganizationSummary> byTaxId(String taxId, String realm) {
        enforceRateLimit();
        return toSummaries(repository.findByTaxId(taxId, realm));
    }

    /**
     * The fallback for when the person onboarding does not have the tax id to
     * hand. Both the stored name and the query are transliterated, so a
     * Cyrillic name is findable by a Latin query and the other way round.
     */
    public List<OrganizationSummary> byName(String name, String city, String realm) {
        enforceRateLimit();
        return toSummaries(repository.findByNameSimilarity(
                Transliterator.toSearchForm(name),
                Transliterator.toSearchForm(city),
                similarityThreshold,
                maxResults,
                realm));
    }

    private void enforceRateLimit() {
        if (!rateLimiter.tryAcquire(currentCaller())) {
            throw new RateLimitExceededException();
        }
    }

    private static String currentCaller() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? UNKNOWN_CALLER : authentication.getName();
    }

    private static List<OrganizationSummary> toSummaries(
            List<OrganizationSearchRepository.SearchRow> rows) {
        return rows.stream()
                .map(row -> new OrganizationSummary(
                        row.orgId(),
                        row.name(),
                        row.city(),
                        ContactMasker.maskEmail(row.contactEmail()),
                        ContactMasker.maskPhone(row.contactPhone()),
                        row.hasMembersInRealm(),
                        row.score()))
                .toList();
    }
}
