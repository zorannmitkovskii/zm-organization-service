package zm.organization.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zm.organization.common.BaseEntity;

/**
 * A legal entity or brand. A chain with three cafés is one organization with
 * three {@link zm.organization.location.Location}s.
 *
 * <p>Identity data only — see the class-level rule in the README.
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
public class Organization extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /**
     * Lowercase Latin transliteration of {@link #name}, maintained by the
     * service layer and covered by the trigram index. Never returned to
     * callers — it exists only so search works across scripts.
     */
    @Column(name = "search_name")
    private String searchName;

    private String legalName;

    /** ЕДБ. Nullable, but unique when present — the deduplication key. */
    @Column(name = "tax_id")
    private String taxId;

    private String vatNumber;

    private String contactEmail;

    private String contactPhone;

    private String website;

    /** S3 key only. Products upload to their own bucket and send the key. */
    @Column(name = "logo_key")
    private String logoKey;

    @Column(nullable = false)
    private String defaultLang;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status;

    /** Which product created this record: menu-app, event-app, presmetko. */
    @Column(name = "created_by_app", nullable = false)
    private String createdByApp;
}
