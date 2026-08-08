# Craftwise

Works out the cheapest way to obtain a craftable item in Final Fantasy XIV, by combining recipe
data from XIVAPI with live market-board prices from Universalis.

For every ingredient, and every ingredient of every ingredient, it asks the same question — is it
cheaper to buy this outright, or to craft it from its own components? The answer is a decision
tree, a total, and a shopping list.

```
.
├─ docker-compose.yaml     Postgres, Redis, Adminer, RedisInsight
├─ .env                    Shared configuration (gitignored)
├─ backend/                Spring Boot 4 API   → backend/README.md
└─ frontend/               React + Vite SPA    → frontend/README.md
```

## Quick start

```bash
# 1. Infrastructure, from the repo root
docker compose up -d

# 2. API on http://localhost:8090
cd backend && ./mvnw spring-boot:run

# 3. UI on http://localhost:5173
cd frontend && npm install && npm run dev
```

The backend reads the root `.env` itself, so no variables need exporting first.

### Seeding the database

The app is useless until the game data is imported. Both syncs are admin-only, and are exposed on
the Admin page in the UI:

1. **Worlds and data centers** — `POST /api/v1/admin/sync/worlds`. Run this first: world
   validation and every selector in the UI depend on it.
2. **Items and recipes** — `POST /api/v1/admin/sync/recipe`. Runs in the background; the Admin
   page polls it. Expect roughly 14k recipes and 13k items.

New accounts are created as `USER`. To grant yourself `ADMIN`, update the row directly — Adminer
is at `localhost:8888`:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your-username';
```

## How it works

```
     XIVAPI                    Universalis
   (recipes, items)          (live market prices)
         │                            │
         │ bulk sync                  │ per request, cached 15 min in Redis
         ▼                            ▼
   ┌──────────────────────────────────────────┐
   │  Spring Boot API            Postgres     │
   │  · recipe tree traversal    Redis        │
   │  · buy vs craft decision                 │
   │  · JWT auth                              │
   └──────────────────────────────────────────┘
                      │ REST
                      ▼
              React SPA (Vite)
```

The interesting part is the traversal. A recipe's ingredients may themselves be craftable, so the
tree is walked breadth-first — one database query per level rather than per item — and every node
independently decides buy or craft. A cheap intermediate can sit under an expensive parent, and
the parent's cost reflects whichever was cheaper for the child.

Recipe yield makes this non-linear: wanting 4 of something a recipe makes 3 at a time is two
crafts and the ingredients for six, not four. Costs are totals for the quantity needed, so a
parent can sum its children directly.

## Ports

| Service | URL |
|---|---|
| Frontend (Vite) | http://localhost:5173 |
| Backend (Spring Boot) | http://localhost:8090 |
| Postgres | localhost:5433 |
| Redis | localhost:6379 |
| Adminer | http://localhost:8888 |
| RedisInsight | http://localhost:5540 |

## Configuration

Everything lives in the root `.env`, which is gitignored. Docker Compose reads it automatically,
and the backend imports it via `spring.config.import`. The full variable table is in
[backend/README.md](backend/README.md).

The frontend needs no configuration in development — Vite proxies `/api` to the backend, so
requests are same-origin. Set `VITE_API_BASE_URL` only when pointing at a deployed API.

## Working on it

```bash
cd backend  && ./mvnw test          # 57 tests
cd backend  && ./mvnw test -Pintegration   # adds live Universalis tests
cd frontend && npm run build        # tsc + vite build
cd frontend && npm run lint
```

Both projects document their reasoning in doc comments above functions rather than inline. If a
decision looks arbitrary, the docblock usually explains what broke when it was done the other way.

### IntelliJ

Open `backend/pom.xml` as the Maven project. A project configured against the pre-split layout has
stale module paths, and the symptom is misleading: IntelliJ decides the source root is
`backend/src`, computes every package as `main.java.com...`, and offers a quick-fix that rewrites
your `package` statements to match. Decline it — the prompt means the source root is wrong, not
the code.
