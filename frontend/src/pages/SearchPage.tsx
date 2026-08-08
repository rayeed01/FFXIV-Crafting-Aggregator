import * as React from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Calculator, Hammer, ScrollText, Search as SearchIcon } from 'lucide-react'
import { api } from '@/lib/api'
import { useAsync, useDebounced } from '@/hooks/useAsync'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { ItemIcon } from '@/components/ItemIcon'
import { JobIcon } from '@/components/JobIcon'
import { jobName } from '@/lib/jobs'
import { EmptyState, ErrorState, PageHeader } from '@/components/states'
import type { ItemDto, RecipeSummaryDto } from '@/types/api'

/** One row of the merged view: an item, plus its recipe when one exists. */
interface SearchResult {
  key: string
  name: string
  iconUrl: string | null
  xivapiId: number | null
  canBeCrafted: boolean
  recipeId: string | null
  job: string | null
  level: number | null
}

/**
 * Merges the item and recipe result sets into one row per item.
 *
 * Items and recipes come from separate endpoints with no shared id - RecipeSummaryDto carries only
 * the result item's name, not its xivapiId - so the name is the join key, normalised to stop case
 * or spacing differences splitting one item into two rows.
 *
 * A recipe with no matching item is kept rather than discarded: both endpoints cap at 50, so a
 * recipe can match while its item falls outside the item cap, and dropping it would make the
 * result set look arbitrary. Such a row has no xivapiId and so cannot offer a cost link.
 *
 * The presence of a recipe is treated as craftability regardless of what the item row claimed.
 * Craftable items sort first, being the ones the calculator can act on.
 */
function mergeResults(items: ItemDto[], recipes: RecipeSummaryDto[]): SearchResult[] {
  const byName = new Map<string, SearchResult>()
  const normalise = (name: string) => name.trim().toLowerCase()

  for (const item of items) {
    byName.set(normalise(item.name), {
      key: item.id,
      name: item.name,
      iconUrl: item.iconUrl,
      xivapiId: item.xivapiId,
      canBeCrafted: item.canBeCrafted,
      recipeId: null,
      job: null,
      level: null,
    })
  }

  for (const recipe of recipes) {
    const key = normalise(recipe.resultItemName)
    const existing = byName.get(key)

    if (existing) {
      existing.recipeId = recipe.id
      existing.job = recipe.job
      existing.level = recipe.level
      existing.canBeCrafted = true
    } else {
      byName.set(key, {
        key: recipe.id,
        name: recipe.resultItemName,
        iconUrl: recipe.resultItemIconUrl,
        xivapiId: null,
        canBeCrafted: true,
        recipeId: recipe.id,
        job: recipe.job,
        level: recipe.level,
      })
    }
  }

  return [...byName.values()].sort((a, b) => {
    if (a.canBeCrafted !== b.canBeCrafted) return a.canBeCrafted ? -1 : 1
    return a.name.localeCompare(b.name)
  })
}

/**
 * Unified search over items and recipes.
 *
 * The query lives in the URL so a search is shareable and survives a reload, and returning from a
 * recipe restores what was typed.
 *
 * Both endpoints are awaited in a single loader so the merged list updates atomically; resolving
 * them separately would make rows appear and then rearrange as the second response landed.
 *
 * The level column is blank for items with no recipe, and also for level 0, which is absent data
 * rather than a real level - 735 recipes carry it.
 */
export function SearchPage() {
  const [params, setParams] = useSearchParams()
  const query = params.get('q') ?? ''

  const [draft, setDraft] = React.useState(query)
  const debounced = useDebounced(draft, 250)

  React.useEffect(() => {
    const next = new URLSearchParams()
    if (debounced) next.set('q', debounced)
    setParams(next, { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced])

  const enabled = debounced.trim().length > 0

  const { data, error, loading, reload } = useAsync(
    async (signal) => {
      const [items, recipes] = await Promise.all([
        api.items.search(debounced, signal),
        api.recipes.search(debounced, signal),
      ])
      return mergeResults(items, recipes)
    },
    [debounced],
    enabled,
  )

  const results = data ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Browse"
        description="Search the synced catalogue. Craftable items can be priced; everything else is here as an ingredient."
      />

      <div className="relative">
        <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Search items and recipes…"
          className="h-11 pl-9"
          autoFocus
          aria-label="Search items and recipes"
        />
      </div>

      {!enabled && (
        <EmptyState
          icon={SearchIcon}
          title="Start typing to search"
          description="No account needed to browse."
        />
      )}

      {enabled && loading && (
        <div className="space-y-2">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      )}

      {enabled && error && <ErrorState error={error} onRetry={reload} />}

      {enabled && !loading && !error && results.length === 0 && (
        <EmptyState title="Nothing matched" description="Try a shorter or differently spelled term." />
      )}

      {enabled && !loading && !error && results.length > 0 && (
        <>
          <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-card">
            {results.map((result) => (
              <li key={result.key} className="flex items-center gap-3 p-3">
                <ItemIcon src={result.iconUrl} alt={result.name} />

                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{result.name}</p>
                  <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    {result.job ? (
                      <>
                        <JobIcon craftType={result.job} />
                        {jobName(result.job)}
                      </>
                    ) : (
                      'Material'
                    )}
                  </p>
                </div>

                <span className="tabular w-14 shrink-0 text-right text-sm text-muted-foreground">
                  {result.level !== null && result.level > 0 ? `Lv ${result.level}` : ''}
                </span>

                {result.canBeCrafted && (
                  <Badge variant="craft" className="hidden sm:inline-flex">
                    <Hammer className="size-3" />
                    Craftable
                  </Badge>
                )}

                <div className="flex shrink-0 items-center gap-1">
                  {result.recipeId && (
                    <Button size="sm" variant="ghost" asChild>
                      <Link to={`/recipes/${result.recipeId}`} aria-label={`Recipe for ${result.name}`}>
                        <ScrollText />
                        <span className="hidden sm:inline">Recipe</span>
                      </Link>
                    </Button>
                  )}
                  {result.xivapiId !== null && (
                    <Button size="sm" variant="outline" asChild>
                      <Link
                        to={`/craft-cost/${result.xivapiId}`}
                        aria-label={`Craft cost for ${result.name}`}
                      >
                        <Calculator />
                        <span className="hidden sm:inline">Cost</span>
                      </Link>
                    </Button>
                  )}
                </div>
              </li>
            ))}
          </ul>

          {results.length >= 50 && (
            <p className="text-center text-xs text-muted-foreground">
              Showing the first 50 matches — refine your search to narrow it down.
            </p>
          )}
        </>
      )}
    </div>
  )
}
