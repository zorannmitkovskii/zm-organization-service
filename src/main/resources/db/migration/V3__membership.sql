-- ============================================================================
-- V3 — membership and invites.
--
-- Each SaaS product has its own Keycloak realm, so the same person holds a
-- different userId in menu-app and in event-app. Membership is therefore
-- (org_id, realm, user_id) — one row per product — and linking a person across
-- products happens through an invite code, not through account linking.
-- ============================================================================

CREATE TABLE organization_members (
    id          UUID        PRIMARY KEY,
    org_id      UUID        NOT NULL REFERENCES organizations (id),
    realm       VARCHAR(64) NOT NULL,
    user_id     VARCHAR(64) NOT NULL,
    role        VARCHAR(16) NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP,
    CONSTRAINT ux_members_org_realm_user UNIQUE (org_id, realm, user_id)
);

-- The check endpoint is the most-called in the service: every product hits it
-- before each protected operation. This index matches its exact lookup.
CREATE INDEX ix_members_lookup ON organization_members (org_id, realm, user_id);

CREATE TABLE member_invites (
    id                 UUID         PRIMARY KEY,
    org_id             UUID         NOT NULL REFERENCES organizations (id),
    -- SHA-256 of the plaintext code. A deterministic hash rather than a salted
    -- one because claiming has to find the row by code; the code is short-lived
    -- and single-organization, so the trade-off is acceptable.
    code_hash          VARCHAR(64)  NOT NULL,
    role               VARCHAR(16)  NOT NULL,
    created_by_member  UUID         REFERENCES organization_members (id),
    expires_at         TIMESTAMP    NOT NULL,
    consumed_at        TIMESTAMP,
    max_uses           INT          NOT NULL,
    use_count          INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP
);

CREATE UNIQUE INDEX ux_invites_code_hash ON member_invites (code_hash);
CREATE INDEX ix_invites_org_id ON member_invites (org_id);
