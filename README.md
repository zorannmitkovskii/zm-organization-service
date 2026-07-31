# zm-organization-service

Platform registry of **business identity** for the ZM platform: organizations,
their locations, and who is allowed to act on their behalf.

Port **8484** · package root `zm.organization` · database `org_db`.

## The boundary

This service stores **identity data only**. Products, menus, portfolios,
articles and themes live in the product that owns them — there is deliberately
no shared `Product` concept on the platform.

| Belongs here | Belongs in the product |
|---|---|
| Legal name, tax id (ЕДБ), contact, logo key | Menu items (zm-menu-service) |
| Physical locations, working hours | Vendor portfolio (ivy-events-be) |
| Membership `(orgId, realm, userId, role)` | Invoice articles (presmetko) |

Consequence: a restaurant that registers on the menu product shows up as a
candidate vendor in Ivy without re-entering its details, and vice versa.

## Membership across realms

Each SaaS product has its own Keycloak realm, so the same person has a
different `userId` in `menu-app` and in `event-app`. Membership is therefore
`(orgId, realm, userId, role)` — one row per product — and linking across
products happens through an **invite/claim code**, not account linking.

## IAM onboarding

Zero Keycloak code. `iam-manifest.yml` declares the one thing this service
needs (a confidential service-account client in the `zm-services` realm) and
`zm-iam-provisioning-client` ships it to `zm-iam-service` on
`ApplicationReadyEvent`. Re-applying an unchanged manifest is a NOOP.

## Local development

Fast loop — Postgres plus this service, no IAM:

```bash
docker compose -f docker-compose.local.yml up --build
curl http://localhost:8484/actuator/health
```

Provisioning logs a failure and continues here (`LOG_AND_CONTINUE`), because
there is no IAM in this compose file. To exercise the real manifest path, run
the full stack from `zm-local-setup/`, which provides Keycloak and
`zm-iam-service`:

```bash
cd ../zm-local-setup
docker compose up -d --build zm-iam-service zm-organization-service
curl http://localhost:8484/actuator/health
```

That stack creates `org_db` through the one-shot `org-db-bootstrap` service
rather than through `postgres/init/01-init.sql`, because the init script only
runs when the `pg_data` volume is empty — on a machine that already ran the
stack, a newly added database would otherwise never appear. The bootstrap step
is idempotent, so it is safe on every `up`.

Running from an IDE against the shared Postgres:

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

## Tests

```bash
mvn test      # unit only (surefire, *Test.java)
mvn verify    # + integration (failsafe, *IT.java, Testcontainers Postgres)
```

Integration tests start a real Postgres and run Flyway against it. They switch
provisioning off per class — the `test` profile itself keeps provisioning **on**
because that profile also runs the deployed test environment.

## Configuration

| Variable | Default | Notes |
|---|---|---|
| `SERVER_PORT` | `8484` | |
| `DB_URL` | `jdbc:postgresql://localhost:5432/org_db` | |
| `DB_USER` / `DB_PASSWORD` | `org_user` / `org_pass` | |
| `IAM_BASE_URL` | `http://localhost:8383` (prod/test: `http://ivy-iam:8383`) | Feeds `iam.provisioning.base-url` |
| `IAM_PROVISIONING_TOKEN` | `local-dev-token` (prod/test: none) | Plaintext half of the bootstrap pair |
| `IAM_PROVISIONING_ENABLED` | `true` | Set `false` where no IAM exists |
| `IAM_PROVISIONING_FAILURE_MODE` | `FAIL_FAST` (local: `LOG_AND_CONTINUE`) | |
| `IAM_INTERNAL_ISSUER_URI` | `http://localhost:8181/realms/zm-services` | Token issuer for `/internal/**` |
| `IAM_INTERNAL_JWK_SET_URI` | *(same realm, `/protocol/openid-connect/certs`)* | Fetched lazily, not at startup |
| `ORG_TAX_ID_PATTERN` | `^[0-9]{13}$` | Macedonian ЕДБ; change per jurisdiction |
| `ORG_INVITE_TTL_DAYS` | `7` | Default invite lifetime |
| `ORG_MEMBERSHIP_CACHE_TTL` | `45` | Seconds; evictions are immediate and exact |
| `ORG_SEARCH_THRESHOLD` | `0.3` | pg_trgm similarity floor |
| `ORG_SEARCH_MAX_RESULTS` | `5` | |
| `ORG_SEARCH_RATE_MAX` / `_WINDOW` | `30` / `60` | Searches per caller per window |

Every property resolves to something, and `ProvisioningConfigGuard` rejects an
unresolved `${...}` placeholder at startup with a message naming the missing
variable. This is a direct response to the 2026-07-25 production incident where
a missing `IAM_BASE_URL` bound as the literal string `${IAM_BASE_URL}` — Spring
Boot's binder ignores unresolvable placeholders — and only surfaced later as
`URISyntaxException: Illegal character in path at index 1`.

## Bootstrap token

Generate the pair with `zm-iam-service/scripts/generate-provisioning-token.sh
zm-organization-service`. The plaintext goes into this service's environment as
`IAM_PROVISIONING_TOKEN`; the argon2 hash goes into IAM's environment as
`IAM_PROVISIONING_TOKEN_ZM_ORGANIZATION_SERVICE`. Never the other way round.

## API

All under `/internal/**` — machine-to-machine. Every call needs a
client-credentials JWT from the `zm-services` realm carrying the `org-client`
realm role. `/actuator/health`, `/actuator/info` and `/actuator/prometheus`
stay open for the container healthcheck and the metrics scraper.

| Method | Path |
|---|---|
| `POST` | `/internal/organizations` |
| `GET` `PATCH` `DELETE` | `/internal/organizations/{id}` |
| `GET` | `/internal/organizations/search?taxId=` · `?name=&city=&realm=` |
| `POST` `GET` | `/internal/organizations/{orgId}/locations` |
| `PATCH` `DELETE` | `/internal/locations/{id}` |
| `POST` `GET` | `/internal/organizations/{orgId}/members` |
| `PATCH` `DELETE` | `/internal/members/{id}` |
| `GET` | `/internal/members/check?orgId=&realm=&userId=` |
| `POST` | `/internal/organizations/{orgId}/invites` |
| `POST` | `/internal/invites/claim` |

**Authorisation philosophy.** This service trusts backends: anyone holding
`org-client` sees every organization. Semantic authorisation — "may this user
edit that menu" — belongs to the product, which asks
`/internal/members/check` and decides for itself. Scoping the registry per
service would defeat the point of having one.

**Membership spans realms.** Each product has its own Keycloak realm, so the
same person holds a different `userId` in each. Membership is
`(orgId, realm, userId, role)` — one row per product — and linking a person
across products happens through an invite code, never account linking. The
last owner of an organization can be neither removed nor demoted.

**Search masks and throttles.** Contact details come back as `k***@domain` and
`+389*****456`; results are capped at five and each caller gets a fixed number
of searches per minute. Search answers questions about organizations the
caller has no membership in, so both limits are load-bearing.

**Cyrillic and Latin find each other.** `search_name` holds a Latin
transliteration maintained by the service, and queries are transliterated the
same way — `панорама скопје` and `panorama skopje` share no trigrams at all
otherwise.

Three behaviours worth knowing before calling it:

- **`DELETE` on an organization suspends it.** The row stays and `GET` keeps
  returning it with `status: SUSPENDED`, because products hold `orgId` as a
  foreign reference. Locations, which nothing outside this service references,
  are deleted outright.
- **`PATCH` is partial and cannot clear a field.** A `null` in the payload is
  indistinguishable from an absent key; send an empty string to clear.
  `taxId`, `status` and `createdByApp` are not updatable at all.
- **A duplicate `taxId` returns 409 with `existingOrgId`.** That id is the
  point: the calling product shows "this company already exists — ask its
  owner for an invite" instead of creating a duplicate. Uniqueness is enforced
  by a partial unique index, so concurrent creates resolve to the same answer.

There is no "list all organizations" endpoint. Enumerating the registry is a
data-leak surface; ORG-05 adds search with masking and rate limiting.

## Deviations from ORG-01

Two, both agreed before implementation:

1. **Docker Hub instead of GHCR.** Matches `ivy-events-be` and
   `zm-iam-service`; a second registry adds a credential without adding value.
   The rollback mechanism ORG-01 mentions is not implemented here — it is
   worth its own ticket covering all three services.
2. **Package root `zm.organization` instead of `org.ivyinc.organization`.**
   This is a platform service like `zm.iam`, not an Ivy product.

One more, smaller: `docker-compose.local.yml` contains Postgres and this
service only, rather than duplicating Keycloak and IAM definitions that already
exist in `zm-local-setup`.
