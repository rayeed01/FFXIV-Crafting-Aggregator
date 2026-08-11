import * as React from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  Coins,
  Loader2,
  Pencil,
  Plus,
  Search as SearchIcon,
  Trash2,
  TriangleAlert,
} from 'lucide-react'
import { toast } from 'sonner'
import { ApiError, api } from '@/lib/api'
import { useAsync, useDebounced } from '@/hooks/useAsync'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { SavedCraftFormFields, useSavedCraftForm } from '@/components/SavedCraftForm'
import { RecipeQuantityRow } from '@/components/RecipeQuantityRow'
import { PendingChangesBar } from '@/components/PendingChangesBar'
import { usePendingQuantities } from '@/hooks/usePendingQuantities'
import { JobIcon } from '@/components/JobIcon'
import { jobName } from '@/lib/jobs'
import { CraftCostTree } from '@/components/CraftCostTree'
import { EmptyState, ErrorState, PageHeader } from '@/components/states'
import { formatGil } from '@/lib/format'
import type { SavedCraftCostDto, SavedCraftDto } from '@/types/api'

/**
 * A single crafting list: its recipes, its market, and its combined cost.
 *
 * Costing is triggered explicitly rather than on page load, because it fans out to Universalis for
 * every ingredient in every recipe. Any change to the contents or the market clears a previously
 * computed total, so a stale figure is never shown against edited contents.
 */
export function SavedCraftDetailPage() {
  const { savedCraftId } = useParams<{ savedCraftId: string }>()

  const { data: craft, error, loading, reload } = useAsync(
    (signal) => api.savedCrafts.byId(savedCraftId!, signal),
    [savedCraftId],
    Boolean(savedCraftId),
  )

  const [cost, setCost] = React.useState<SavedCraftCostDto | null>(null)
  const [costError, setCostError] = React.useState<ApiError | null>(null)
  const [costing, setCosting] = React.useState(false)
  const [adding, setAdding] = React.useState(false)
  const [editing, setEditing] = React.useState(false)

  /** Any change to contents or scope invalidates a previously computed total. */
  const invalidateCost = React.useCallback(() => setCost(null), [])

  async function calculateCost() {
    if (!savedCraftId) return
    setCosting(true)
    setCostError(null)
    try {
      setCost(await api.savedCrafts.cost(savedCraftId))
    } catch (err) {
      setCostError(err instanceof ApiError ? err : new ApiError(0, 'Could not price this list.'))
    } finally {
      setCosting(false)
    }
  }

  if (loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (error) return <ErrorState error={error} onRetry={reload} />
  if (!craft) return null

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" asChild className="-ml-2">
        <Link to="/lists">
          <ArrowLeft /> All lists
        </Link>
      </Button>

      <PageHeader
        title={craft.title}
        description={craft.notes ?? undefined}
        actions={
          <>
            <Button variant="ghost" onClick={() => setEditing(true)}>
              <Pencil /> Edit
            </Button>
            <Button variant="outline" onClick={() => setAdding(true)}>
              <Plus /> Add recipes
            </Button>
            <Button onClick={calculateCost} disabled={costing || craft.recipes.length === 0}>
              {costing ? <Loader2 className="animate-spin" /> : <Coins />}
              Calculate cost
            </Button>
          </>
        }
      />

      <div className="flex flex-wrap items-center gap-2">
        <Badge variant="secondary">Priced against {craft.priceScope}</Badge>
        <Badge variant="outline">{craft.dataCenter}</Badge>
        {craft.world ? (
          <Badge variant="outline">{craft.world}</Badge>
        ) : (
          <Badge variant="outline">Any world</Badge>
        )}
      </div>

      {costError && <ErrorState error={costError} onRetry={calculateCost} />}
      {cost && <CostReport cost={cost} showWorld={!craft.world} />}

      <RecipeSection craft={craft} onChanged={reload} invalidateCost={invalidateCost} onAdd={() => setAdding(true)} />

      <AddRecipesDialog
        open={adding}
        onOpenChange={setAdding}
        craft={craft}
        onAdded={() => {
          invalidateCost()
          reload()
        }}
      />

      <EditListDialog
        open={editing}
        onOpenChange={setEditing}
        craft={craft}
        onSaved={() => {
          invalidateCost()
          reload()
        }}
      />
    </div>
  )
}

/**
 * The list's recipes, with quantities editable in bulk.
 *
 * Edits accumulate in {@link usePendingQuantities} and are saved from a bar pinned to the
 * viewport, so the save control is always in the same place and never appears beneath the cursor
 * that just changed a value.
 */
function RecipeSection({
  craft,
  onChanged,
  invalidateCost,
  onAdd,
}: {
  craft: SavedCraftDto
  onChanged: () => void
  invalidateCost: () => void
  onAdd: () => void
}) {
  const pendingSave = React.useCallback(() => {
    invalidateCost()
    onChanged()
  }, [invalidateCost, onChanged])

  const pending = usePendingQuantities(craft.id, pendingSave)

  if (craft.recipes.length === 0) {
    return (
      <section className="space-y-3">
        <h2 className="text-lg font-semibold tracking-tight">Recipes</h2>
        <EmptyState
          title="No recipes in this list"
          description="Add recipes to price them together."
          action={
            <Button onClick={onAdd}>
              <Plus /> Add recipes
            </Button>
          }
        />
      </section>
    )
  }

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold tracking-tight">
        Recipes <span className="text-muted-foreground">({craft.recipes.length})</span>
      </h2>
      <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-card">
        {craft.recipes.map((entry) => (
          <li key={entry.recipe.id}>
            <RecipeQuantityRow
              craftId={craft.id}
              recipeId={entry.recipe.id}
              name={entry.recipe.resultItem.name}
              job={entry.recipe.job}
              level={entry.recipe.level}
              quantity={entry.quantity}
              value={pending.valueFor(entry.recipe.id, entry.quantity)}
              dirty={pending.isDirty(entry.recipe.id)}
              onQuantityChange={(next) => pending.setQuantity(entry.recipe.id, next, entry.quantity)}
              onRemoved={() => {
                invalidateCost()
                onChanged()
              }}
            />
          </li>
        ))}
      </ul>

      <PendingChangesBar
        variant="pinned"
        count={pending.count}
        saving={pending.saving}
        onSave={pending.saveAll}
        onDiscard={pending.discard}
      />
    </section>
  )
}

function CostReport({ cost, showWorld }: { cost: SavedCraftCostDto; showWorld: boolean }) {
  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total craft cost</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="tabular text-2xl font-semibold">{formatGil(cost.totalCraftCost)}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total buy cost</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="tabular text-2xl font-semibold">{formatGil(cost.totalBuyCost)}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Savings</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="tabular text-2xl font-semibold">{formatGil(cost.savings)}</p>
            <p className="mt-1 text-xs text-muted-foreground">Priced against {cost.scope}</p>
          </CardContent>
        </Card>
      </div>

      {cost.unobtainableItems.length > 0 && (
        <div className="rounded-xl border border-warning/40 bg-warning/10 p-4">
          <p className="flex items-center gap-2 text-sm font-medium">
            <TriangleAlert className="size-4 text-warning" />
            {cost.unobtainableItems.length} item
            {cost.unobtainableItems.length === 1 ? '' : 's'} could not be priced
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            These have no market listing in {cost.scope}, so the totals above exclude them:{' '}
            {cost.unobtainableItems.join(', ')}
          </p>
        </div>
      )}

      {cost.items.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold tracking-tight">Breakdown</h2>
          <div className="space-y-3">
            {cost.items.map((item, index) => (
              <CraftCostTree key={`${item.itemXivapiId}-${index}`} root={item} showWorld={showWorld} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

function EditListDialog({
  open,
  onOpenChange,
  craft,
  onSaved,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  craft: SavedCraftDto
  onSaved: () => void
}) {
  const form = useSavedCraftForm({
    open,
    initial: {
      title: craft.title,
      notes: craft.notes ?? '',
      dataCenter: craft.dataCenter,
      world: craft.world ?? '',
    },
  })

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    await form.submit(async (values) => {
      await api.savedCrafts.update(craft.id, {
        title: values.title,
        notes: values.notes || null,
        dataCenter: values.dataCenter,
        world: values.world || null,
      })
      toast.success('List updated')
      onOpenChange(false)
      onSaved()
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit list</DialogTitle>
          <DialogDescription>
            Changing the market re-prices the list next time you calculate.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <SavedCraftFormFields form={form} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={form.submitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={form.submitting || !form.values.title || !form.values.dataCenter}>
              {form.submitting && <Loader2 className="animate-spin" />}
              Save changes
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/**
 * Multi-select recipe picker: stage a selection, then add it all in one request, which
 * AddRecipeRequest already accepts as a list.
 *
 * The dialog is a bounded flex column so each section scrolls within its own limits. As a single
 * scrolling block the staged selection grew without limit and pushed the search results off the
 * bottom once a handful were picked.
 *
 * The results list needs min-h-0 as well as flex-1: a flex child defaults to min-height:auto and
 * would otherwise refuse to shrink below its content, defeating the overflow entirely.
 */
function AddRecipesDialog({
  open,
  onOpenChange,
  craft,
  onAdded,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  craft: SavedCraftDto
  onAdded: () => void
}) {
  const [query, setQuery] = React.useState('')
  const debounced = useDebounced(query, 250)
  const [selected, setSelected] = React.useState<Map<string, { name: string; quantity: number }>>(new Map())
  const [submitting, setSubmitting] = React.useState(false)

  React.useEffect(() => {
    if (open) {
      setQuery('')
      setSelected(new Map())
    }
  }, [open])

  const { data, loading } = useAsync(
    (signal) => api.recipes.search(debounced, signal),
    [debounced],
    open && debounced.trim().length > 0,
  )

  const alreadyAdded = new Set(craft.recipes.map((r) => r.recipe.id))

  function toggle(recipeId: string, name: string) {
    setSelected((prev) => {
      const next = new Map(prev)
      if (next.has(recipeId)) next.delete(recipeId)
      else next.set(recipeId, { name, quantity: 1 })
      return next
    })
  }

  function setQuantity(recipeId: string, quantity: number) {
    setSelected((prev) => {
      const next = new Map(prev)
      const entry = next.get(recipeId)
      if (entry) next.set(recipeId, { ...entry, quantity })
      return next
    })
  }

  async function addAll() {
    if (selected.size === 0) return
    setSubmitting(true)
    try {
      await api.savedCrafts.addRecipes(craft.id, {
        recipes: [...selected.entries()].map(([recipeId, { quantity }]) => ({ recipeId, quantity })),
      })
      toast.success(`Added ${selected.size} recipe${selected.size === 1 ? '' : 's'}`)
      onOpenChange(false)
      onAdded()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Could not add those recipes.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[85dvh] max-w-2xl flex-col gap-4">
        <DialogHeader className="shrink-0">
          <DialogTitle>Add recipes</DialogTitle>
          <DialogDescription>
            Select as many as you like, set quantities, then add them all at once.
          </DialogDescription>
        </DialogHeader>

        <div className="relative shrink-0">
          <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search recipes…"
            className="pl-9"
            autoFocus
          />
        </div>

        {/* Two independently-scrolling columns. The selection lives in its own column rather than
            above the results, so ticking a box cannot push the list someone is reading. */}
        <div className="grid min-h-0 flex-1 gap-3 sm:grid-cols-[1fr_18rem]">
        <div className="min-h-0 overflow-y-auto rounded-md border border-border">
          {debounced.trim().length === 0 && (
            <p className="p-4 text-sm text-muted-foreground">Start typing to find recipes.</p>
          )}
          {loading && <p className="p-4 text-sm text-muted-foreground">Searching…</p>}
          {!loading && debounced.trim().length > 0 && (data?.length ?? 0) === 0 && (
            <p className="p-4 text-sm text-muted-foreground">No recipes matched.</p>
          )}
          <ul className="divide-y divide-border">
            {(data ?? []).map((recipe) => {
              const added = alreadyAdded.has(recipe.id)
              const checked = selected.has(recipe.id)
              return (
                <li key={recipe.id}>
                  <label
                    className={`flex cursor-pointer items-center gap-3 p-3 hover:bg-secondary/60 ${
                      added ? 'cursor-not-allowed opacity-60' : ''
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      disabled={added}
                      onChange={() => toggle(recipe.id, recipe.resultItemName)}
                      className="size-4 accent-[var(--primary)]"
                    />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium">{recipe.resultItemName}</span>
                      <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        <JobIcon craftType={recipe.job} />
                        {jobName(recipe.job)}
                        {recipe.level > 0 && <span>· Lv {recipe.level}</span>}
                      </span>
                    </span>
                    {added && <Badge variant="secondary">In list</Badge>}
                  </label>
                </li>
              )
            })}
          </ul>
        </div>

          <div className="flex min-h-0 flex-col rounded-md border border-border bg-secondary/30">
            <div className="flex shrink-0 items-center justify-between gap-2 border-b border-border px-3 py-2">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                Selected ({selected.size})
              </p>
              {selected.size > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-6 px-2 text-xs text-muted-foreground"
                  onClick={() => setSelected(new Map())}
                >
                  Clear
                </Button>
              )}
            </div>

            {selected.size === 0 ? (
              <p className="p-3 text-xs text-muted-foreground">
                Tick recipes on the left to add them.
              </p>
            ) : (
              <ul className="min-h-0 flex-1 space-y-2 overflow-y-auto p-3">
                {[...selected.entries()].map(([recipeId, entry]) => (
                  <li key={recipeId} className="flex items-center gap-2 text-sm">
                    <span className="min-w-0 flex-1 truncate" title={entry.name}>
                      {entry.name}
                    </span>
                    <Input
                      type="number"
                      min={1}
                      max={999}
                      value={entry.quantity}
                      onChange={(e) =>
                        setQuantity(recipeId, Math.max(1, Math.min(999, Number(e.target.value) || 1)))
                      }
                      className="h-8 w-16"
                      aria-label={`Quantity for ${entry.name}`}
                    />
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-8 shrink-0"
                      aria-label={`Deselect ${entry.name}`}
                      onClick={() => toggle(recipeId, entry.name)}
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>

        <DialogFooter className="shrink-0">
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitting}>
            Cancel
          </Button>
          <Button onClick={addAll} disabled={submitting || selected.size === 0}>
            {submitting && <Loader2 className="animate-spin" />}
            Add {selected.size > 0 ? `${selected.size} recipe${selected.size === 1 ? '' : 's'}` : 'recipes'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
