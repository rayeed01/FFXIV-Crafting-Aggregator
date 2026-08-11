import { Check, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

/**
 * Save/discard control for pending quantity edits.
 *
 * Deliberately the only place saving happens, and always in the same position, so the control
 * never materialises under the cursor that just made the edit.
 *
 * Two placements. `pinned` fixes it to the bottom of the viewport, for the list detail page where
 * only one list can be edited and the rows may scroll past. `inline` renders it in flow, for the
 * lists index where several cards can be open at once and a floating bar would not say which list
 * it belonged to.
 *
 * Renders nothing when there is nothing to save, so it never occupies space idly.
 */
export function PendingChangesBar({
  count,
  saving,
  onSave,
  onDiscard,
  variant = 'inline',
}: {
  count: number
  saving: boolean
  onSave: () => void
  onDiscard: () => void
  variant?: 'inline' | 'pinned'
}) {
  if (count === 0) return null

  return (
    <div
      role="status"
      className={cn(
        'flex items-center gap-3 rounded-lg border border-border bg-card px-4 py-2.5 shadow-lg',
        variant === 'pinned' &&
          'fixed bottom-4 left-1/2 z-40 w-[calc(100%-2rem)] max-w-md -translate-x-1/2',
      )}
    >
      <span className="flex-1 text-sm">
        <span className="font-medium">{count}</span> unsaved change{count === 1 ? '' : 's'}
      </span>

      <Button variant="ghost" size="sm" onClick={onDiscard} disabled={saving}>
        Discard
      </Button>
      <Button size="sm" onClick={onSave} disabled={saving}>
        {saving ? <Loader2 className="animate-spin" /> : <Check />}
        Save all
      </Button>
    </div>
  )
}
