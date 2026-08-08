# Craftwise — Frontend

React single-page app for the [Craftwise API](../backend/README.md).

## Stack

- **React 19** + **TypeScript**, built with **Vite 8**
- **Tailwind CSS v4** — CSS-first, so there is no `tailwind.config.js`; tokens live in
  `src/index.css` under `@theme`
- **shadcn/ui**-style primitives in `src/components/ui`, built on Radix
- **React Router 7**, **sonner** for toasts, **lucide-react** for icons

## Running

```bash
npm install
npm run dev       # http://localhost:5173
npm run build     # tsc -b && vite build → dist/
npm run lint      # oxlint
npm run preview   # serve the production build
```

The dev server proxies `/api` to `http://localhost:8090`, so the backend must be running. Requests
are same-origin in development, which keeps CORS out of the picture entirely.

To point at a deployed API, set `VITE_API_BASE_URL` in `.env.local`.

## Layout

```
components/
  ui/                 Primitives: button, card, dialog, select, input, badge…
  layout/             App shell, header, navigation
  CraftCostTree       Recursive buy-vs-craft breakdown and shopping list
  WorldPicker         Paired DC/world picker, and the single scope select
  SavedCraftForm      Shared fields for the create and edit list dialogs
  RecipeQuantityRow   Editable quantity row, used by both list views
  ItemIcon/JobIcon    Icons with fallbacks
  states              PageHeader, ErrorState, EmptyState
context/              AuthContext (JWT session), ThemeContext (light/dark)
hooks/                useAsync (fetch + abort), useDebounced, useWorlds (cached)
lib/                  api.ts (typed client), format.ts, icons.ts, jobs.ts, utils.ts
pages/                One file per route
types/api.ts          TypeScript mirrors of the backend's Java records
```

## Pages

| Route | Auth | Purpose |
|---|---|---|
| `/search` | public | Unified item + recipe search |
| `/recipes/:id` | public | Recipe and its materials |
| `/craft-cost/:itemId` | public | The calculator; one item at a time signed out |
| `/lists` | required | Crafting lists, expandable to edit quantities |
| `/lists/:id` | required | One list, its recipes and combined cost |
| `/profile` | required | Account, default market, appearance, activity |
| `/admin` | ADMIN | Sync triggers and status |

## Notes on the less obvious decisions

**Icons.** The backend stores XIVAPI's raw game path (`ui/icon/025000/025301.tex`) — a texture in
the game's own format, which no browser can render. `lib/icons.ts` converts it to XIVAPI's asset
endpoint. Already-usable http(s) URLs pass through untouched, so a future sync storing real URLs
needs no change here.

**Jobs.** The backend stores XIVAPI's *CraftType* (`Smithing`), not the job that performs it
(`Blacksmith`). `lib/jobs.ts` maps between them and supplies the icon id. All eight Disciples of
the Hand are covered and the set is closed, so an unmatched value means bad data rather than a
missing entry.

**Auth.** The JWT is held in memory and mirrored to localStorage. On boot a stored token is only
trusted once `/users/me` confirms it — otherwise an expired token renders a logged-in shell with
no data behind it. A 401 from anywhere clears the session exactly once.

**World and data-center pairing.** The backend rejects a world that does not belong to the chosen
data center. The picker filters worlds to the selected data center and drops one that is no longer
valid, so the invalid combination cannot be built in the first place.

**Null renders as `—`, never `0`.** The API uses null for "no market listing". In combined totals a
single unknown makes the whole sum unknown, rather than a partial figure that reads as complete.

**Costing is explicit.** List pricing fans out to Universalis per ingredient, so it runs on a
button press rather than on every page view. Any edit clears a previously computed total.

**Theming.** Light by default with a persisted toggle. An inline script in `index.html` applies the
class before first paint so a dark-mode reload never flashes white. Every token is defined on
`:root` and redefined under `.dark` — no colour exists in only one theme.

`index.html` also carries `<meta name="darkreader-lock">`. Dark Reader assumes a page is light-only
and inverts it; on top of a real dark theme that flattened the table borders to invisible and
turned the dialog scrim opaque. The tag is that extension's documented opt-out for sites that
implement their own dark mode. Remove it if you would rather the extension take over.

**Admin nav is presentation only.** It is hidden unless `/users/me` reports `ADMIN`, but the
backend enforces the role regardless of what the menu shows.

## Code style

Documentation goes in doc comments above functions and components, not inside their bodies. Where
a line looks arbitrary, the docblock explains what broke when it was written the other way — the
column widths in `CraftCostTree`, the `min-h-0` in the add-recipes dialog, and the dependency array
in `SavedCraftForm` are all load-bearing for reasons that are not visible locally.

## Known advisory

`npm audit` reports GHSA-qwww-vcr4-c8h2 against `react-router`. It concerns **RSC mode** — server
components with server actions. This is a client-only SPA against a separate API, so the vulnerable
path is not reachable. The installed version is already the latest published, and
`npm audit fix --force` would downgrade to an older major.
