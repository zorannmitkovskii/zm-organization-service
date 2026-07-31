-- ============================================================================
-- V2 — the registry core: organizations and their physical locations.
--
-- Identity data only. Products, menus, portfolios and articles live in the
-- product that owns them and must never appear here.
-- ============================================================================

CREATE TABLE organizations (
    id              UUID         PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    legal_name      VARCHAR(255),
    tax_id          VARCHAR(32),
    vat_number      VARCHAR(32),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(32),
    website         VARCHAR(255),
    logo_key        VARCHAR(512),
    default_lang    VARCHAR(8)   NOT NULL DEFAULT 'mk',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by_app  VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP
);

-- Partial unique index rather than a plain UNIQUE constraint: several
-- organizations may legitimately have no tax id yet (registered before the
-- paperwork), but a tax id that IS present identifies exactly one company.
-- This index is what makes concurrent creates safe — the loser gets a
-- constraint violation, which the service turns into 409 + existingOrgId.
CREATE UNIQUE INDEX ux_organizations_tax_id
    ON organizations (tax_id)
    WHERE tax_id IS NOT NULL;

CREATE TABLE locations (
    id             UUID         PRIMARY KEY,
    org_id         UUID         NOT NULL REFERENCES organizations (id),
    name           VARCHAR(200) NOT NULL,
    address        VARCHAR(255),
    city           VARCHAR(120),
    country        VARCHAR(2),
    geo_lat        NUMERIC(9, 6),
    geo_lng        NUMERIC(9, 6),
    contact_phone  VARCHAR(32),
    working_hours  JSONB,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP
);

CREATE INDEX ix_locations_org_id ON locations (org_id);
