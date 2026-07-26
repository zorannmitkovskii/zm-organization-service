-- ============================================================================
-- V1 — baseline
--
-- ORG-01 deliberately ships no domain tables: Organization and Location arrive
-- in ORG-02, membership and invites in ORG-03. This migration exists so the
-- Flyway schema history is created on first boot and every later migration
-- has a known starting point.
--
-- pg_trgm is enabled here rather than in the ORG-05 migration that needs it:
-- CREATE EXTENSION requires elevated privileges that the service's own DB user
-- may not hold in a managed Postgres, so it belongs in the one migration an
-- operator is most likely to run with a superuser during initial setup.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;
