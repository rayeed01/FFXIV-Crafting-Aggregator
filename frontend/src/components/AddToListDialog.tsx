import * as React from 'react'
import { Link } from 'react-router-dom'
import { Loader2, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError, api } from '@/lib/api'
import { useAsync } from '@/hooks/useAsync'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

/**
 * Adds a single recipe to one of the user's lists, from anywhere a recipe is shown.
 *
 * The lists are fetched when the dialog opens rather than on mount, so browsing costs nothing
 * for someone who never adds anything.
 *
 * A user with no lists gets a link to create one instead of an empty dropdown, since an empty
 * selector gives no indication of what is wrong.
 *
 * Adding upserts, so choosing a list that already contains the recipe replaces its quantity
 * rather than failing. The dialog says so, because "add" would otherwise imply the two amounts
 * are summed.
 */
export function AddToListDialog({
  open,
  onOpenChange,
  recipeId,
  recipeName,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  recipeId: string
  recipeName: string
}) {
  const [listId, setListId] = React.useState('')
  const [quantity, setQuantity] = React.useState(1)
  const [submitting, setSubmitting] = React.useState(false)

  const { data: lists, loading, error } = useAsync(
    (signal) => api.savedCrafts.list(signal),
    [open],
    open,
  )

  React.useEffect(() => {
    if (open) {
      setQuantity(1)
      setListId('')
    }
  }, [open])

  // Preselect when there is only one list, which removes the only decision in that case.
  React.useEffect(() => {
    if (lists?.length === 1) setListId(lists[0].id)
  }, [lists])

  const chosen = lists?.find((l) => l.id === listId)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!listId) return

    setSubmitting(true)
    try {
      await api.savedCrafts.addRecipes(listId, { recipes: [{ recipeId, quantity }] })
      toast.success(`Added ${recipeName} to ${chosen?.title ?? 'the list'}`)
      onOpenChange(false)
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Could not add to that list.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Add to list</DialogTitle>
          <DialogDescription className="truncate">{recipeName}</DialogDescription>
        </DialogHeader>

        {loading && <Skeleton className="h-9 w-full" />}

        {error && (
          <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm">
            {error.message}
          </p>
        )}

        {!loading && !error && lists?.length === 0 && (
          <div className="space-y-3 rounded-md border border-dashed border-border p-4 text-center">
            <p className="text-sm text-muted-foreground">You do not have any lists yet.</p>
            <Button asChild size="sm" onClick={() => onOpenChange(false)}>
              <Link to="/lists">
                <Plus /> Create a list
              </Link>
            </Button>
          </div>
        )}

        {!loading && !error && (lists?.length ?? 0) > 0 && (
          <form onSubmit={submit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="target-list">List</Label>
              <Select value={listId || undefined} onValueChange={setListId}>
                <SelectTrigger id="target-list">
                  <SelectValue placeholder="Choose a list" />
                </SelectTrigger>
                <SelectContent>
                  {lists!.map((list) => (
                    <SelectItem key={list.id} value={list.id}>
                      {list.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {chosen && (
                <p className="text-xs text-muted-foreground">
                  Priced against {chosen.priceScope} · {chosen.recipeCount} recipe
                  {chosen.recipeCount === 1 ? '' : 's'}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="add-quantity">Quantity</Label>
              <Input
                id="add-quantity"
                type="number"
                min={1}
                max={999}
                value={quantity}
                onChange={(e) => setQuantity(Math.max(1, Math.min(999, Number(e.target.value) || 1)))}
                className="w-24"
              />
              <p className="text-xs text-muted-foreground">
                If this recipe is already in the list, its quantity is replaced.
              </p>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitting}>
                Cancel
              </Button>
              <Button type="submit" disabled={submitting || !listId}>
                {submitting && <Loader2 className="animate-spin" />}
                Add
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  )
}
