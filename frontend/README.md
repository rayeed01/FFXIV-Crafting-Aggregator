# FFXIV Crafting Aggregator — Frontend

React single-page app for the [backend API](../backend/README.md).

## Stack

- **React 19** + **TypeScript**, built with **Vite 8**
- **Tailwind CSS v4** (CSS-first config — there is no `tailwind.config.js`; tokens live in
  `src/index.css` under `@theme`)
- **shadcn/ui**-style primitives in `src/components/ui`, built on Radix
- **React Router 7** for routing, **sonner** for toasts, **lucide-react** for icons

## Running

```bash
npm install
npm run dev      # http://localhost:5173
```

The dev server proxies `/api` to `http://localhost:8090`, so the backend must be running.
Requests are same-origin in development, which keeps CORS out of the picture entirely.

```bash
npm run build    # tsc -b && vite build  ->  dist/
npm run lint     # oxlint
npm run preview  # serve the production build locally
```

To point at a non-local API, set `VITE_API_BASE_URL` (e.g. in `.env.local`):

```
VITE_API_BASE_URL=https://api.example.com
```

## Layout

```
src/
├─ components/
│  ├─ ui/              Reusable primitives (button, card, dialog, select, …)
│  ├─ layout/          App shell, header, nav
│  ├─ CraftCostTree    Recursive buy-vs-craft breakdown + shopping list
│  ├─ WorldPicker      Paired DC/world picker and the single "price against" scope select
│  └─ states           PageHeader, ErrorState, EmptyState
├─ context/            AuthContext (JWT session), ThemeContext (light/dark)
├─ hooks/              useAsync (fetch + abort), useDebounced, useWorlds (cached server list)
├─ lib/                api.ts (typed client), format.ts (gil/date), utils.ts (cn)
├─ pages/              One file per route
└─ types/api.ts        TypeScript mirrors of the backend's Java records
```

## Notes on a few decisions

**Theming.** Light is the default, with a header toggle that persists to `localStorage`. An
inline script in `index.html` applies the class before first paint so a dark-mode reload never
flashes white. Every colour token is defined on `:root` and *redefined* under `.dark` — no colour
exists only in one theme.

**Auth.** The JWT is held in memory and mirrored to `localStorage`. On boot the stored token is
only trusted once `GET /users/me` confirms it, so an expired token cannot render a logged-in
shell with no data. Any `401` from any request clears the session once, via a handler the API
client calls.

**World/data-center pairing.** The backend rejects a world that does not belong to the chosen
data center (`WorldDataCenterMismatchException`). The picker filters worlds to the selected data
center and clears a now-invalid world, so that combination cannot be constructed in the UI.

**Admin nav.** Hidden unless `GET /users/me` reports `role: "ADMIN"`. This is presentation only —
the backend still enforces `hasRole("ADMIN")` on `/api/v1/admin/**` regardless.

**Null vs zero.** The API uses `null` for "no market listing / unobtainable". Costs render as
`—`, never `0`, since a missing price is not a free item. In the shopping-list totals a single
unknown makes the whole sum unknown rather than silently partial.

**Costing is explicit.** Saved-craft pricing hits Universalis for every ingredient, so it runs on
a button press rather than on every page view.

## Known advisory

`npm audit` reports GHSA-qwww-vcr4-c8h2 against `react-router`. It concerns **RSC mode** (server
components with server actions); this is a client-only SPA against a separate API, so the
vulnerable path is not reachable. The installed version is already the latest published, and
`npm audit fix --force` would downgrade to an older major.
