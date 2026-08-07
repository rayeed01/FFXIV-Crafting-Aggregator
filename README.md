# FFXIV Crafting Aggregator — Backend

A Spring Boot API that aggregates FFXIV recipe data (from XIVAPI) and live market
prices (from Universalis) to compute the cheapest way to obtain a craftable item —
recursively deciding, ingredient by ingredient, whether to buy from the market board
or craft from sub-components. Users can also save crafting lists and get their total
cost calculated against their preferred world or data center.

## Tech stack

- **Java 25**, **Spring Boot 4.0.6**
- Spring Data JPA + **PostgreSQL**
- **Redis** for caching (market prices)
- Spring Security with **JWT** auth (`jjwt`) and **password4j** (BCrypt) for hashing
- Spring RestClient for outbound calls to XIVAPI and Universalis
- Lombok, Bean Validation
- JUnit 5 / Spring Boot Test for tests

## Architecture

- `client` — outbound HTTP clients (`XivapiClient`, `UniversalisClient`) and their DTOs
- `controller` — REST endpoints
- `service` — business logic (recipe sync, craft cost calculation, market prices, saved crafts, auth, users)
- `sync` — background bulk-sync orchestration for pulling recipes from XIVAPI (`BulkSyncRunner`, `RecipeSyncProcessor`)
- `domain/entity` — JPA entities (`Item`, `Recipe`, `RecipeMaterials`, `World`, `DataCenter`, `User`, `SavedCraft`, `SavedCraftRecipes`)
- `domain/dto` — request/response payloads
- `mapper` — entity ↔ DTO mapping
- `repository` — Spring Data JPA repositories
- `security` — JWT filter/service, `UserDetailsService`, auth entry points
- `config` — Security, CORS, and async configuration
- `exception` — domain exceptions + a `GlobalExceptionHandler` that maps them to `ErrorResponse`

## Prerequisites

- JDK 25
- Docker (for Postgres, Redis, and optional Adminer/RedisInsight via `docker-compose.yaml`)
- Maven Wrapper is included (`./mvnw` / `mvnw.cmd`), no local Maven install required

## Configuration

Environment variables are read by `src/main/resources/application.properties`.
`.env` (gitignored) holds these for `docker compose`; fill in real values there:

| Variable | Required | Default | Notes |
|---|---|---|---|
| `POSTGRES_USER` | yes | — | |
| `POSTGRES_PASSWORD` | yes | — | |
| `POSTGRES_DB` | no | `craft` | |
| `POSTGRES_HOST` | no | `localhost` | |
| `POSTGRES_PORT` | no | `5433` | Host-published port for the `db` container |
| `JWT_SECRET` | yes | — | HS256 key, needs ≥256 bits. Generate with `openssl rand -base64 64`. Never commit a real value. |
| `JWT_EXPIRATION` | no | `86400000` (24h, ms) | |
| `REDIS_HOST` | no | `localhost` | |
| `REDIS_PORT` | no | `6379` | |
| `XIVAPI_BASE_URL` | no | `https://v2.xivapi.com` | |
| `UNIVERSALIS_BASE_URL` | no | `https://universalis.app` | |

`docker-compose.yaml` reads these from a `.env` file in the project root automatically.
Spring Boot does **not** read `.env` itself — export the variables into your shell, or
set them as run configuration environment variables in your IDE, before running the app.

## Running locally

1. Start infrastructure:
   ```
   docker compose up -d
   ```
   This brings up Postgres (`localhost:5433`), Redis (`localhost:6379`), Adminer
   (`localhost:8888`, DB admin UI), and RedisInsight (`localhost:5540`).

2. Export the environment variables listed above (or configure them in your IDE run config).

3. Run the app:
   ```
   ./mvnw spring-boot:run
   ```
   The API listens on `http://localhost:8090`.

4. Populate reference data (both are admin-only, so log in as an admin user first — see [Auth](#auth--roles)):
   - `POST /api/v1/admin/sync/worlds` — syncs `World`/`DataCenter` from Universalis
   - `POST /api/v1/admin/sync/recipe` — bulk syncs `Item`/`Recipe`/`RecipeMaterials` from XIVAPI (async; poll `GET /api/v1/admin/sync/recipe` for status)

## Running tests

```
./mvnw test
```

Integration tests (tagged `integration`, e.g. `UniversalisClientIntegrationTest` which hits
the live Universalis API) are excluded by default and only run with the `integration` Maven profile:

```
./mvnw test -Pintegration
```

## Auth & roles

JWT bearer auth, stateless sessions. Two roles: `USER` (default on registration) and `ADMIN`.

- `POST /api/v1/auth/**` — open to everyone
- `GET /api/v1/items/**`, `GET /api/v1/recipes/**` — open to everyone
- `/api/v1/admin/**` — requires `ROLE_ADMIN`
- everything else — requires a valid JWT

Send the token as `Authorization: Bearer <token>` on subsequent requests.

CORS is currently locked to `http://localhost:5173` and `http://localhost:3000` (see
`CorsConfig`) for `GET`, `POST`, `PATCH`, `DELETE`, `OPTIONS` — update this before deploying
a frontend from another origin.

## API overview

Base path: `/api/v1`

### Auth (`/auth`)

| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/auth/register` | `{ username, email, password, defaultDataCenter, defaultWorld }` | Create a user, returns `AuthResponse` (JWT) |
| POST | `/auth/login` | `{ username, password }` | Returns `AuthResponse` (JWT) |

### Users (`/users`) — auth required

| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/users/me` | — | Current user profile |
| PATCH | `/users/me/defaults` | `{ defaultDataCenter, defaultWorld }` | Update default world/DC |

### Items (`/items`) — public

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/items/{id}` | — | Item by internal UUID |
| GET | `/items` | `search` | Search items by name |

### Recipes (`/recipes`) — public

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/recipes/{id}` | — | Recipe by internal UUID |
| GET | `/recipes` | `search` and/or `job` | Search recipes by name, or filter by crafting job |

### Craft cost (`/craft-cost`) — auth required

| Method | Path | Query | Description |
|---|---|---|---|
| GET | `/craft-cost/{itemXivapiId}` | `quantity` (default 1), `scope` (world **or** data center name) | Recursively computes cheapest buy-vs-craft decision tree (`CraftCostNode`) for the item, priced against `scope` |

### Saved crafts (`/saved-crafts`) — auth required, owner-scoped

| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/saved-crafts` | `CreateSavedCraftRequest` | Create a saved craft list |
| GET | `/saved-crafts` | — | List current user's saved crafts (summaries) |
| GET | `/saved-crafts/{id}` | — | Get one saved craft |
| PATCH | `/saved-crafts/{id}` | `UpdateSavedCraftRequest` | Update title/notes/scope |
| DELETE | `/saved-crafts/{id}` | — | Delete a saved craft |
| POST | `/saved-crafts/{id}/recipes` | `AddRecipeRequest` | Add recipes to a saved craft |
| DELETE | `/saved-crafts/{id}/recipes` | `RemoveRecipeRequest` | Remove recipes from a saved craft |
| GET | `/saved-crafts/{id}/cost` | — | Compute total cost for all recipes in the saved craft |

### Admin (`/admin`) — `ROLE_ADMIN` required

| Method | Path | Description |
|---|---|---|
| POST | `/admin/sync/recipe` | Starts an async bulk sync of items/recipes from XIVAPI, returns `SyncStatus` (202 Accepted) |
| GET | `/admin/sync/recipe` | Poll current `SyncStatus` |
| POST | `/admin/sync/worlds` | Syncs worlds/data centers from Universalis, returns `GameServerSyncResult` (202 Accepted) |

## Error handling

`GlobalExceptionHandler` maps domain exceptions (e.g. `ItemNotFoundException`,
`RecipeNotFoundException`, `SavedCraftNotFoundException`, `UnauthorizedSavedCraftAccessException`,
`UnknownWorldException`, `UnknownDataCenterException`, `GameServerDataNotSyncedException`,
`UniversalisException`) to a consistent `ErrorResponse` body with an appropriate HTTP status.