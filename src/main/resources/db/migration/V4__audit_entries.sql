-- ============================================================================
-- V4 — audit trail for write operations.
--
-- Organization records are tax-relevant (ЕДБ, legal names), so "who changed
-- what, when" has to be answerable. Reads are not audited: the members/check
-- endpoint alone would produce more rows than every other operation combined,
-- and it reveals nothing that a write does not.
--
-- `detail` holds the NAMES of fields that changed, never their values — the
-- audit trail must not become a second copy of the personal data.
-- ============================================================================

CREATE TABLE audit_entries (
    id           UUID         PRIMARY KEY,
    caller       VARCHAR(128) NOT NULL,
    operation    VARCHAR(64)  NOT NULL,
    target_type  VARCHAR(16)  NOT NULL,
    target_id    UUID,
    detail       JSONB,
    success      BOOLEAN      NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP
);

CREATE INDEX ix_audit_target ON audit_entries (target_type, target_id);
CREATE INDEX ix_audit_created_at ON audit_entries (created_at DESC);
