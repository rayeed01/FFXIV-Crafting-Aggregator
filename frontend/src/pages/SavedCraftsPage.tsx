import * as React from 'react'
import { Link } from 'react-router-dom'
import { Bookmark, ChevronDown, Loader2, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError, api } from '@/lib/api'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/context/AuthContext'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
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
import { EmptyState, ErrorState, PageHeader } from '@/components/states'
import { formatRelative } from '@/lib/format'
import { cn } from '@/lib/utils'
import type { SavedCraftSummaryDto } from '@/types/api'

/**
 * Index of the user's crafting lists.
 *
 * The grid uses items-start so a row does not stretch every card to match its tallest sibling,
 * which would otherwise make a neighbouring card grow whenever one was expanded.
 */
export function SavedCraftsPage() {
  const { user } = useAuth()
  const { data, error, loading, reload } = useAsync((signal) => api.savedCrafts.list(signal), [])
  const [creating, setCreating] = React.useState(false)

  return (
    <div className="space-y-6">
      <PageHeader
        title="Lists"
        description="Reusable sets of recipes, priced together against one market."
        actions={
          <Button onClick={() => setCreating(true)}>
            <Plus /> New list
          </Button>
        }
      />

      {loading && (
        <div className="grid gap-3 sm:grid-cols-2">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-28" />
          ))}
        </div>
      )}

      {error && <ErrorState error={error} onRetry={reload} />}

      {!loading && !error && (data?.length ?? 0) === 0 && (
        <EmptyState
          icon={Bookmark}
          title="No lists yet"
          description="Create a list to price several recipes together."
          action={
            <Button onClick={() => setCreating(true)}>
              <Plus /> New list
            </Button>
          }
        />
      )}

      {!loading && !error && (data?.length ?? 0) > 0 && (
        <div className="grid items-start gap-3 sm:grid-cols-2">
          {data!.map((craft) => (
            <SavedCraftCard key={craft.id} craft={craft} onChanged={reload} />
          ))}
        </div>
      )}

      <CreateListDialog
        open={creating}
        onOpenChange={setCreating}
        defaultDataCenter={user?.defaultDataCenter ?? ''}
        defaultWorld={user?.defaultWorld ?? ''}
        onCreated={reload}
      />
    </div>
  )
}

/**
 * One list, as a card that can expand to reveal and edit its recipe quantities.
 *
 * Contents are fetched on first expand rather than eagerly: the summary endpoint returns only a
 * count, so loading every card's recipes up front would be one request per card on page load.
 *
 * The whole card is a link, laid over the content rather than wrapping it, so that the delete
 * button and the quantity inputs are never nested inside an anchor. Those controls are lifted
 * above the overlay so they remain clickable.
 */
function SavedCraftCard({ craft, onChanged }: { craft: SavedCraftSummaryDto; onChanged: () => void }) {
  const [deleting, setDeleting] = React.useState(false)
  const [confirming, setConfirming] = React.useState(false)
  const [expanded, setExpanded] = React.useState(false)

  const { data: detail, loading: detailLoading, error: detailError, reload } = useAsync(
    (signal) => api.savedCrafts.byId(craft.id, signal),
    [craft.id],
    expanded,
  )

  const afterSave = React.useCallback(() => {
    reload()
    onChanged()
  }, [reload, onChanged])

  const pending = usePendingQuantities(craft.id, afterSave)

  async function handleDelete() {
    setDeleting(true)
    try {
      await api.savedCrafts.remove(craft.id)
      toast.success(`Deleted “${craft.title}”`)
      setConfirming(false)
      onChanged()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Could not delete this list.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <>
      <Card className="relative transition-shadow focus-within:ring-2 focus-within:ring-ring hover:shadow-md">
        <CardContent className="flex h-full flex-col gap-3 p-5">
          <Link
            to={`/lists/${craft.id}`}
            className="absolute inset-0 rounded-xl focus:outline-none"
            aria-label={`Open ${craft.title}`}
          />

          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <p className="truncate font-medium">{craft.title}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                Updated {formatRelative(craft.updatedAt)}
              </p>
            </div>
            <Button
              variant="ghost"
              size="icon"
              aria-label={`Delete ${craft.title}`}
              onClick={() => setConfirming(true)}
              className="relative z-10 text-muted-foreground hover:text-destructive"
            >
              <Trash2 />
            </Button>
          </div>

          {craft.notes && <p className="line-clamp-2 text-sm text-muted-foreground">{craft.notes}</p>}

          <div className="mt-auto flex flex-wrap items-center gap-2">
            <Badge variant="secondary">{craft.priceScope}</Badge>
            <Badge variant="outline">
              {craft.recipeCount} recipe{craft.recipeCount === 1 ? '' : 's'}
            </Badge>

            {craft.recipeCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setExpanded((v) => !v)}
                aria-expanded={expanded}
                className={cn(
                  'relative z-10 ml-auto h-7 gap-1 px-2 text-xs',
                  pending.count > 0 ? 'text-primary' : 'text-muted-foreground',
                )}
              >
                {expanded ? 'Hide' : 'Items'}
                {/* Surfaced on the collapsed toggle too, so folding a card away cannot hide
                    unsaved edits with no indication they are still there. */}
                {pending.count > 0 && <span>· {pending.count} unsaved</span>}
                <ChevronDown className={cn('size-3.5 transition-transform', expanded && 'rotate-180')} />
              </Button>
            )}
          </div>

          {expanded && (
            <div className="relative z-10 border-t border-border pt-2">
              {detailLoading && <Skeleton className="h-10 w-full" />}
              {detailError && (
                <p className="py-2 text-sm text-destructive">{detailError.message}</p>
              )}
              {detail && (
                <>
                  <ul className="divide-y divide-border">
                    {detail.recipes.map((entry) => (
                      <li key={entry.recipe.id}>
                        <RecipeQuantityRow
                          compact
                          craftId={craft.id}
                          recipeId={entry.recipe.id}
                          name={entry.recipe.resultItem.name}
                          job={entry.recipe.job}
                          level={entry.recipe.level}
                          quantity={entry.quantity}
                          value={pending.valueFor(entry.recipe.id, entry.quantity)}
                          dirty={pending.isDirty(entry.recipe.id)}
                          onQuantityChange={(next) =>
                            pending.setQuantity(entry.recipe.id, next, entry.quantity)
                          }
                          onRemoved={() => {
                            reload()
                            onChanged()
                          }}
                        />
                      </li>
                    ))}
                  </ul>

                  {/* Inline rather than pinned: several cards can be open at once, and a floating
                      bar would not say which list it was about to save. */}
                  {pending.count > 0 && (
                    <div className="pt-2">
                      <PendingChangesBar
                        count={pending.count}
                        saving={pending.saving}
                        onSave={pending.saveAll}
                        onDiscard={pending.discard}
                      />
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={confirming} onOpenChange={setConfirming}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete “{craft.title}”?</DialogTitle>
            <DialogDescription>
              This removes the list and its recipes. It cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirming(false)} disabled={deleting}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deleting}>
              {deleting && <Loader2 className="animate-spin" />}
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}

function CreateListDialog({
  open,
  onOpenChange,
  defaultDataCenter,
  defaultWorld,
  onCreated,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  defaultDataCenter: string
  defaultWorld: string
  onCreated: () => void
}) {
  const form = useSavedCraftForm({
    open,
    initial: { title: '', notes: '', dataCenter: defaultDataCenter, world: defaultWorld },
  })

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    await form.submit(async (values) => {
      await api.savedCrafts.create({
        title: values.title,
        notes: values.notes || null,
        dataCenter: values.dataCenter,
        world: values.world || null,
        recipes: [],
      })
      toast.success(`Created “${values.title}”`)
      onOpenChange(false)
      onCreated()
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New list</DialogTitle>
          <DialogDescription>Pick the market this list should be priced against.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <SavedCraftFormFields form={form} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={form.submitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={form.submitting || !form.values.title || !form.values.dataCenter}>
              {form.submitting && <Loader2 className="animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
