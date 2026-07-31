-- ============================================================================
-- V5 — search columns and trigram indexes (ORG-05).
--
-- search_name / search_city hold a lowercase Latin transliteration of the
-- display value, written by the application (see Transliterator). Queries are
-- transliterated the same way, so "панорама скопје" and "panorama skopje" hit
-- the same rows — which they cannot do on the raw columns, since the two forms
-- share no trigrams.
--
-- Doing the fold in the application rather than in SQL keeps one authoritative
-- mapping: a Postgres function would have to be kept byte-identical to the Java
-- one, and any drift would show up as silently missing search results.
-- ============================================================================

ALTER TABLE organizations ADD COLUMN search_name VARCHAR(400);
ALTER TABLE locations     ADD COLUMN search_city VARCHAR(240);

CREATE INDEX ix_organizations_search_name_trgm
    ON organizations USING GIN (search_name gin_trgm_ops);

CREATE INDEX ix_locations_search_city
    ON locations (search_city);
