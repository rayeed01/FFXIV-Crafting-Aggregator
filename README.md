# FFXIV Crafting Aggregator

Aggregates FFXIV recipe data (XIVAPI) and live market-board prices (Universalis) to work out the
cheapest way to obtain a craftable item — recursively deciding, ingredient by ingredient, whether
to buy it outright or craft it from components.

```
.
├─ docker-compose.yaml     Postgres, Redis, Adminer, RedisInsight
├─ .env                    Shared config (gitignored)
├─ backend/                Spring Boot 4 API  → see backend/README.md
└─ frontend/               React + Vite SPA   → see frontend/README.md
```

## Quick start

```bash
# 1. Infrastructure (from the repo root)
docker compose up -d

# 2. API on http://localhost:8090
#    Export the vars from .env first, or set them in your IDE run config.
cd backend && ./mvnw spring-boot:run

# 3. UI on http://localhost:5173
cd frontend && npm install && npm run dev
```

Then, signed in as an **admin**, seed the database from the Admin page (or by hand):

1. `POST /api/v1/admin/sync/worlds` — worlds and data centers. **Run this first**; world
   validation and every selector in the UI depend on it.
2. `POST /api/v1/admin/sync/recipe` — the item/recipe catalogue. Runs in the background.

New accounts are created with role `USER`. To grant yourself `ADMIN`, update the row directly
(Adminer is at `localhost:8888`):

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your-username';
```

## Configuration

Both services read from the root `.env`. Docker Compose picks it up automatically; Spring Boot
does **not**, so those variables must be exported into the environment or set in your IDE's run
configuration. See [backend/README.md](backend/README.md) for the full variable table.

The frontend needs no configuration in development — Vite proxies `/api` to `http://localhost:8090`,
so requests are same-origin. Set `VITE_API_BASE_URL` only when pointing at a deployed API.

## Ports

| Service | URL |
|---|---|
| Frontend (Vite) | http://localhost:5173 |
| Backend (Spring Boot) | http://localhost:8090 |
| Postgres | localhost:5433 |
| Redis | localhost:6379 |
| Adminer (DB UI) | http://localhost:8888 |
| RedisInsight | http://localhost:5540 |

## Note on IntelliJ

The backend moved into `backend/`, so a project configured against the old layout has stale
module paths. Re-import by opening `backend/pom.xml` as the Maven project.
