import * as React from 'react'
import { Link } from 'react-router-dom'
import { Check, Loader2, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError, api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { JobIcon } from '@/components/JobIcon'
import { jobName } from '@/lib/jobs'
import { cn } from '@/lib/utils'

/**
 * One root recipe in a list, with its quantity editable in place.
 *
 * Saving uses addRecipes rather than a dedicated endpoint: the backend upserts, so re-sending an
 * existing recipeId overwrites its quantity.
 *
 * The draft quantity re-syncs whenever the parent refetches, so a change made elsewhere is not
 * masked by stale local state. Input is clamped to the backend's Min(1)/Max(999) so the request
 * cannot 400.
 *
 * @param onChanged fired after a successful save or removal, for the parent to refetch and drop
 *                  any previously computed cost
 * @param compact   tighter spacing, for the expandable cards on the lists index
 */
export function RecipeQuantityRow({
  craftId,
  recipeId,
  name,
  job,
  level,
  quantity,
  onChanged,
  compact = false,
}: {
  craftId: string
  recipeId: string
  name: string
  job: string
  level: number
  quantity: number
  /** Called after a successful save or removal, so the parent can refetch and drop stale costs. */
  onChanged: () => void
  compact?: boolean
}) {
  const [draft, setDraft] = React.useState(quantity)
  const [saving, setSaving] = React.useState(false)
  const [removing, setRemoving] = React.useState(false)

  React.useEffect(() => setDraft(quantity), [quantity])

  const dirty = draft !== quantity

  async function saveQuantity() {
    setSaving(true)
    try {
      await api.savedCrafts.addRecipes(craftId, { recipes: [{ recipeId, quantity: draft }] })
      toast.success(`${name} set to ×${draft}`)
      onChanged()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Could not update the quantity.')
      setDraft(quantity)
    } finally {
      setSaving(false)
    }
  }

  async function remove() {
    setRemoving(true)
    try {
      await api.savedCrafts.removeRecipes(craftId, { recipeIds: [recipeId] })
      toast.success(`Removed ${name}`)
      onChanged()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Could not remove that recipe.')
      setRemoving(false)
    }
  }

  return (
    <div className={cn('flex items-center gap-3', compact ? 'py-2' : 'p-3')}>
      <div className="min-w-0 flex-1">
        <Link
          to={`/recipes/${recipeId}`}
          className={cn('block truncate font-medium hover:underline', compact && 'text-sm')}
        >
          {name}
        </Link>
        <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <JobIcon craftType={job} />
          {jobName(job)}
          {level > 0 && <span>· Lv {level}</span>}
        </p>
      </div>

      <Input
        type="number"
        min={1}
        max={999}
        value={draft}
        onChange={(e) => setDraft(Math.max(1, Math.min(999, Number(e.target.value) || 1)))}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && dirty) void saveQuantity()
        }}
        className={cn('w-20', compact && 'h-8')}
        aria-label={`Quantity for ${name}`}
        disabled={saving || removing}
      />

      {dirty && (
        <Button size="sm" onClick={saveQuantity} disabled={saving} className={cn(compact && 'h-8')}>
          {saving ? <Loader2 className="animate-spin" /> : <Check />}
          Save
        </Button>
      )}

      <Button
        variant="ghost"
        size="icon"
        aria-label={`Remove ${name}`}
        onClick={remove}
        disabled={removing || saving}
        className={cn('text-muted-foreground hover:text-destructive', compact && 'size-8')}
      >
        {removing ? <Loader2 className="animate-spin" /> : <Trash2 />}
      </Button>
    </div>
  )
}
