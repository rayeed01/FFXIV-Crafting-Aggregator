# Craftwise — Backend

Spring Boot API that aggregates FFXIV recipe data (XIVAPI) and live market-board prices
(Universalis), and computes the cheapest route to a craftable item.

## Stack

- **Java 25**, **Spring Boot 4.0.6**
- Spring Data JPA + **PostgreSQL**
- **Redis** for market-price caching
- Spring Security with **JWT** (`jjwt`) and **password4j** (bcrypt)
- Spring RestClient for outbound calls
- Lombok, Bean Validation
- JUnit 5 / AssertJ / Mockito — 57 tests

## Layout

```
client/      Outbound HTTP to XIVAPI and Universalis, plus their response DTOs
controller/  REST endpoints
service/     Business logic; interfaces here, implementations in service/impl
sync/        Background bulk import from XIVAPI
domain/      JPA entities and the DTOs crossing the API boundary
mapper/      Entity ↔ DTO conversion
repository/  Spring Data repositories
security/    JWT filter and service, UserDetails, auth entry points
exception/   Domain exceptions and the GlobalExceptionHandler
config/      Security, CORS, async
```

## Running

```bash
./mvnw spring-boot:run      # http://localhost:8090
./mvnw test                 # unit and slice tests
./mvnw test -Pintegration   # adds tests that call Universalis for real
```

Postgres and Redis come from the root `docker-compose.yaml`. Integration tests are tagged and
excluded by default, since they depend on a third-party API being up and on real listings existing.

## Configuration

Read from the root `.env` via `spring.config.import`, so no launcher needs the variables exported.
Real environment variables still take precedence, which is what a deployment would supply.

| Variable | Required | Default | Notes |
|---|---|---|---|
| `POSTGRES_USER` | yes | — | |
| `POSTGRES_PASSWORD` | yes | — | |
| `POSTGRES_DB` | no | `craft` | |
| `POSTGRES_HOST` | no | `localhost` | |
| `POSTGRES_PORT` | no | `5433` | Host-published port of the `db` container |
| `JWT_SECRET` | yes | — | HS256, needs ≥256 bits. `openssl rand -base64 64`. Never commit a real one. |
| `JWT_EXPIRATION` | no | `86400000` (24h, ms) | |
| `REDIS_HOST` | no | `localhost` | |
| `REDIS_PORT` | no | `6379` | |
| `XIVAPI_BASE_URL` | no | `https://v2.xivapi.com` | |
| `UNIVERSALIS_BASE_URL` | no | `https://universalis.app` | |

## Auth

JWT bearer tokens, stateless sessions, two roles: `USER` (default) and `ADMIN`.

Read access to game and market data is public, because it is data XIVAPI and Universalis already
serve openly — and because registration needs the world list before any token exists.

| Scope | Access |
|---|---|
| `POST /api/v1/auth/**` | public |
| `GET /api/v1/items/**`, `/recipes/**` | public |
| `GET /api/v1/worlds`, `/data-centers` | public |
| `GET /api/v1/craft-cost/**` | public |
| `/api/v1/admin/**` | `ROLE_ADMIN` |
| everything else | authenticated |

CSRF is disabled: the session is a bearer token rather than a cookie, so there is no ambient
credential for a cross-site post to abuse. CORS allows `localhost:5173` and `localhost:3000`.

> **Not yet done:** `/craft-cost` is public and fans out to Universalis on every call, so the
> protection it actually wants is rate limiting rather than authentication.

## API

Base path `/api/v1`.

### Auth

| Method | Path | Body |
|---|---|---|
| POST | `/auth/register` | `{ username, email, password, defaultDataCenter, defaultWorld }` → JWT |
| POST | `/auth/login` | `{ username, password }` → JWT |

### Users — authenticated

| Method | Path | Description |
|---|---|---|
| GET | `/users/me` | Profile, including `role` and `createdAt` |
| PATCH | `/users/me/defaults` | Update default data center and world |

### Worlds — public

| Method | Path | Description |
|---|---|---|
| GET | `/worlds` | Synced worlds with their data center and region |
| GET | `/data-centers` | Synced data centers |

Both return **503** until `POST /admin/sync/worlds` has run, rather than an empty array, so the
caller can distinguish "not synced yet" from "nothing matched".

### Items and recipes — public

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/items` | `search` | Name substring match, **capped at 50** |
| GET | `/items/{id}` | — | By internal UUID |
| GET | `/recipes` | `search` or `job` | Capped at 50; `job` filters by craft type |
| GET | `/recipes/{id}` | — | Recipe with materials |

The cap matters: uncapped, a one-letter search returned ~7,000 rows and 1.8 MB of JSON.

### Craft cost — public

| Method | Path | Query |
|---|---|---|
| GET | `/craft-cost/{itemXivapiId}` | `quantity` (1–999), `scope`, `quality` |

`scope` accepts either a world or a data center name and is canonicalised server-side. `quality` is
`CHEAPEST` (default), `HQ` or `NQ`, and applies to the requested item only — not to its
ingredients, because an HQ result comes from crafting skill rather than HQ materials.

Each node reports `buyCost`, `craftCost`, `effectiveCost`, the `decision`, both `buyCostNq` and
`buyCostHq`, which quality was used, the world the price came from, and the recipe's job and level.

### Saved crafts — authenticated, owner-scoped

| Method | Path | Description |
|---|---|---|
| POST | `/saved-crafts` | Create a list |
| GET | `/saved-crafts` | Current user's lists (summaries) |
| GET | `/saved-crafts/{id}` | One list with its recipes |
| PATCH | `/saved-crafts/{id}` | Update title, notes, data center, world |
| DELETE | `/saved-crafts/{id}` | Delete |
| POST | `/saved-crafts/{id}/recipes` | Add recipes — **upserts**, so re-sending an id updates its quantity |
| DELETE | `/saved-crafts/{id}/recipes` | Remove recipes |
| GET | `/saved-crafts/{id}/cost` | Cost the whole list together |

### Admin — `ROLE_ADMIN`

| Method | Path | Description |
|---|---|---|
| POST | `/admin/sync/worlds` | Sync worlds and data centers from Universalis |
| POST | `/admin/sync/recipe` | Start the async XIVAPI import (202) |
| GET | `/admin/sync/recipe` | Poll sync status |

## Design notes

**Null is not zero.** An unlisted item has a null cost, never 0. A zero would make it look free and
every craft using it look profitable. The same rule propagates: one unobtainable ingredient makes
the whole craft cost unknown rather than a partial sum that reads as complete.

**HQ and NQ are tracked separately**, each with its own world id, because the two qualities are
frequently cheapest on different worlds — quoting the NQ world for an HQ purchase would send a
buyer to the wrong place. Both live in one Redis entry keyed by scope and item, so the cache is
quality-agnostic and switching quality costs no extra upstream call.

**Prices are cached in Redis for 15 minutes**, with unresolved items held for 7 days since an id
Universalis does not know is unlikely to change soon.

**Quantities merge per result item before pricing.** Two lines in a list can name different recipes
producing the same item, and pricing them separately would be wrong: recipe yield makes cost
non-linear.

**404, not 403, for another user's list.** A 403 confirms the resource exists and allows
enumeration by id.

## Known gaps

- Exceptions thrown inside `JwtAuthFilter` bypass `GlobalExceptionHandler`, so an expired or
  malformed token produces Spring Security's default body rather than an `ErrorResponse`. Clients
  see two different error shapes depending on where the failure happened. Documented in that class.
- `IllegalArgumentException` is mapped to 400, but `NumberFormatException` extends it — a
  server-side bug can be reported to the caller as bad input. A dedicated exception would be
  cleaner.