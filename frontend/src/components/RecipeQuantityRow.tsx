import * as React from 'react'
import { Link } from 'react-router-dom'
import { Loader2, Trash2 } from 'lucide-react'
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
 * The quantity is controlled by the parent, which collects edits across rows and saves them
 * together - see {@code usePendingQuantities}. This row therefore never saves and never shows a
 * save button: a button appearing mid-row landed directly under the cursor that had just changed
 * the value.
 *
 * An edited row is marked with a dot rather than by adding or resizing any control, so the layout
 * cannot shift while it is being used.
 *
 * Removal is immediate and independent of pending edits, since it is unambiguous and reversible
 * by re-adding.
 *
 * @param quantity saved value, used to decide whether the row is dirty
 * @param value    what to display - the pending edit if there is one, otherwise {@code quantity}
 * @param compact  tighter spacing, for the expandable cards on the lists index
 */
export function RecipeQuantityRow({
  craftId,
  recipeId,
  name,
  job,
  level,
  quantity,
  value,
  dirty,
  onQuantityChange,
  onRemoved,
  compact = false,
}: {
  craftId: string
  recipeId: string
  name: string
  job: string
  level: number
  quantity: number
  value: number
  dirty: boolean
  onQuantityChange: (next: number) => void
  onRemoved: () => void
  compact?: boolean
}) {
  const [removing, setRemoving] = React.useState(false)

  async function remove() {
    setRemoving(true)
    try {
      await api.savedCrafts.removeRecipes(craftId, { recipeIds: [recipeId] })
      toast.success(`Removed ${name}`)
      onRemoved()
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
        value={value}
        onChange={(e) => onQuantityChange(Math.max(1, Math.min(999, Number(e.target.value) || 1)))}
        className={cn('w-20', compact && 'h-8', dirty && 'border-primary')}
        aria-label={`Quantity for ${name}`}
        disabled={removing}
      />

      {/* Fixed-width slot so the dot's presence never shifts the row. */}
      <span className="flex w-2 shrink-0 justify-center">
        {dirty && (
          <span
            className="size-2 rounded-full bg-primary"
            title={`Unsaved: was ${quantity}`}
            aria-label="Unsaved change"
          />
        )}
      </span>

      <Button
        variant="ghost"
        size="icon"
        aria-label={`Remove ${name}`}
        onClick={remove}
        disabled={removing}
        className={cn('text-muted-foreground hover:text-destructive', compact && 'size-8')}
      >
        {removing ? <Loader2 className="animate-spin" /> : <Trash2 />}
      </Button>
    </div>
  )
}
