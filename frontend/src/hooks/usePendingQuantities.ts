import * as React from 'react'
import { toast } from 'sonner'
import { ApiError, api } from '@/lib/api'

/**
 * Collects quantity edits across several rows and saves them in one request.
 *
 * Edits are held here rather than in each row so that saving can be a single deliberate action
 * on a bar that never moves, instead of a button appearing inside a row directly under the
 * cursor that just changed it.
 *
 * An edit that returns a row to its saved value is dropped rather than recorded, so typing 3, 5
 * and then 3 again leaves nothing pending and the save bar disappears on its own.
 *
 * Saving is one call: addRecipes upserts and accepts a list, so every changed row goes up
 * together and a partial failure cannot leave some rows saved and others not.
 *
 * @param craftId   the list being edited
 * @param onSaved   called after a successful save, for the parent to refetch and drop stale costs
 */
export function usePendingQuantities(craftId: string, onSaved: () => void) {
  const [pending, setPending] = React.useState<Map<string, number>>(new Map())
  const [saving, setSaving] = React.useState(false)

  const setQuantity = React.useCallback((recipeId: string, next: number, saved: number) => {
    setPending((prev) => {
      const updated = new Map(prev)
      if (next === saved) updated.delete(recipeId)
      else updated.set(recipeId, next)
      return updated
    })
  }, [])

  const discard = React.useCallback(() => setPending(new Map()), [])

  const saveAll = React.useCallback(async () => {
    if (pending.size === 0) return

    setSaving(true)
    try {
      await api.savedCrafts.addRecipes(craftId, {
        recipes: [...pending.entries()].map(([recipeId, quantity]) => ({ recipeId, quantity })),
      })
      toast.success(`Saved ${pending.size} change${pending.size === 1 ? '' : 's'}`)
      setPending(new Map())
      onSaved()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Could not save the changes.')
    } finally {
      setSaving(false)
    }
  }, [craftId, pending, onSaved])

  /** Quantity to display for a row: the pending edit if there is one, otherwise the saved value. */
  const valueFor = React.useCallback(
    (recipeId: string, saved: number) => pending.get(recipeId) ?? saved,
    [pending],
  )

  return {
    count: pending.size,
    isDirty: (recipeId: string) => pending.has(recipeId),
    valueFor,
    setQuantity,
    discard,
    saveAll,
    saving,
  }
}
