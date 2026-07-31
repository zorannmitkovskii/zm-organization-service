package zm.organization.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import zm.organization.common.BaseEntity;
import zm.organization.organization.Organization;

import java.math.BigDecimal;
import java.util.Map;

/** A physical place an organization operates from. */
@Entity
@Table(name = "locations")
@Getter
@Setter
public class Location extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    /** Lowercase Latin transliteration of {@link #city}. See Organization#searchName. */
    @Column(name = "search_city")
    private String searchCity;

    /** ISO 3166-1 alpha-2. */
    private String country;

    @Column(name = "geo_lat")
    private BigDecimal geoLat;

    @Column(name = "geo_lng")
    private BigDecimal geoLng;

    private String contactPhone;

    /**
     * Free-form opening hours, e.g. {@code {"mon-fri": "08:00-23:00"}}. Kept
     * unstructured on purpose: opening-hour conventions differ enough between
     * countries and venue types that a schema here would be wrong within a
     * month, and nothing in this service reasons about the values.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_hours", columnDefinition = "jsonb")
    private Map<String, String> workingHours;

    @Column(nullable = false)
    private boolean active;
}
