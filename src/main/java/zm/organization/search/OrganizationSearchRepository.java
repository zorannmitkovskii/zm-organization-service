package zm.organization.search;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Search runs as native SQL rather than JPQL: it needs pg_trgm's
 * {@code %} operator to hit the GIN index and {@code similarity()} for the
 * score, neither of which JPQL can express.
 */
@Repository
@RequiredArgsConstructor
public class OrganizationSearchRepository {

    /**
     * {@code search_name % :query} is the index-accelerated prefilter; the
     * explicit similarity comparison then applies the configured threshold,
     * which may be stricter than pg_trgm's session default.
     *
     * <p>The city is joined from locations because organizations do not carry
     * one — a chain has several. The lateral picks the oldest location as the
     * one to show, which in practice is the original address.
     */
    private static final String FUZZY_SEARCH = """
            SELECT o.id                       AS org_id,
                   o.name                     AS name,
                   loc.city                   AS city,
                   o.contact_email            AS contact_email,
                   o.contact_phone            AS contact_phone,
                   similarity(o.search_name, :query) AS score,
                   EXISTS (SELECT 1 FROM organization_members m
                            WHERE m.org_id = o.id AND m.realm = :realm) AS has_members
              FROM organizations o
              LEFT JOIN LATERAL (
                   SELECT l.city, l.search_city
                     FROM locations l
                    WHERE l.org_id = o.id
                    ORDER BY l.created_at
                    LIMIT 1
              ) loc ON TRUE
             WHERE o.search_name % :query
               AND similarity(o.search_name, :query) >= :threshold
               AND (CAST(:city AS text) IS NULL OR EXISTS (
                        SELECT 1 FROM locations l2
                         WHERE l2.org_id = o.id
                           AND l2.search_city LIKE CAST(:cityPattern AS text)))
             ORDER BY score DESC, o.name
             LIMIT :limit
            """;

    private static final String EXACT_BY_TAX_ID = """
            SELECT o.id                AS org_id,
                   o.name              AS name,
                   loc.city            AS city,
                   o.contact_email     AS contact_email,
                   o.contact_phone     AS contact_phone,
                   NULL::real          AS score,
                   EXISTS (SELECT 1 FROM organization_members m
                            WHERE m.org_id = o.id AND m.realm = :realm) AS has_members
              FROM organizations o
              LEFT JOIN LATERAL (
                   SELECT l.city FROM locations l
                    WHERE l.org_id = o.id ORDER BY l.created_at LIMIT 1
              ) loc ON TRUE
             WHERE o.tax_id = :taxId
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public List<SearchRow> findByTaxId(String taxId, String realm) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("taxId", taxId)
                .addValue("realm", realm);
        return jdbc.query(EXACT_BY_TAX_ID, params, OrganizationSearchRepository::mapRow);
    }

    public List<SearchRow> findByNameSimilarity(String searchQuery, String searchCity,
                                                double threshold, int limit, String realm) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", searchQuery)
                .addValue("city", searchCity)
                .addValue("cityPattern", searchCity == null ? null : "%" + searchCity + "%")
                .addValue("threshold", threshold)
                .addValue("limit", limit)
                .addValue("realm", realm);
        return jdbc.query(FUZZY_SEARCH, params, OrganizationSearchRepository::mapRow);
    }

    /** Exposes the query plan so a test can prove the GIN index is in use. */
    public String explainFuzzySearch(String searchQuery, double threshold, int limit, String realm) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", searchQuery)
                .addValue("city", null)
                .addValue("cityPattern", null)
                .addValue("threshold", threshold)
                .addValue("limit", limit)
                .addValue("realm", realm);
        List<String> lines = jdbc.queryForList(
                "EXPLAIN " + FUZZY_SEARCH, params, String.class);
        return String.join("\n", lines);
    }

    private static SearchRow mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Double score = rs.getObject("score") == null ? null : rs.getDouble("score");
        return new SearchRow(
                UUID.fromString(rs.getString("org_id")),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("contact_email"),
                rs.getString("contact_phone"),
                score,
                rs.getBoolean("has_members"));
    }

    public record SearchRow(UUID orgId, String name, String city, String contactEmail,
                            String contactPhone, Double score, boolean hasMembersInRealm) {
    }
}
