import * as React from 'react'
import { ChevronRight, Hammer, ShoppingCart, Ban, RefreshCcwDot, MapPin } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { JobIcon } from '@/components/JobIcon'
import { jobName } from '@/lib/jobs'
import { useWorlds } from '@/hooks/useWorlds'
import { formatGil, formatNumber } from '@/lib/format'
import { cn } from '@/lib/utils'
import type { CraftCostNode, Decision } from '@/types/api'

const DECISION_META: Record<
  Decision,
  { label: string; variant: 'buy' | 'craft' | 'unobtainable' | 'cycle'; Icon: typeof Hammer }
> = {
  BUY: { label: 'Buy', variant: 'buy', Icon: ShoppingCart },
  CRAFT: { label: 'Craft', variant: 'craft', Icon: Hammer },
  UNOBTAINABLE: { label: 'Unobtainable', variant: 'unobtainable', Icon: Ban },
  CYCLE: { label: 'Recipe loop', variant: 'cycle', Icon: RefreshCcwDot },
}

/**
 * Buy / craft / unobtainable marker for one row.
 *
 * CYCLE is given a plain label rather than being hidden: the backend emits it when a recipe graph
 * refers back to itself, and a bare em-dash there reads as a loading failure.
 */
export function DecisionBadge({ decision }: { decision: Decision }) {
  const { label, variant, Icon } = DECISION_META[decision]
  return (
    <Badge variant={variant}>
      <Icon className="size-3" />
      {label}
    </Badge>
  )
}

/**
 * HQ/NQ marker.
 *
 * Styled distinctly from the decision colours so the two badge families do not blur together in a
 * dense row.
 */
export function QualityBadge({ quality }: { quality: 'HQ' | 'NQ' }) {
  return (
    <Badge
      variant="outline"
      className={cn(
        'font-mono text-[10px] tracking-wider',
        quality === 'HQ'
          ? 'border-amber-400/60 text-amber-700 dark:text-amber-300'
          : 'border-border text-muted-foreground',
      )}
      title={quality === 'HQ' ? 'High quality listing' : 'Normal quality listing'}
    >
      {quality}
    </Badge>
  )
}

interface NodeRowProps {
  node: CraftCostNode
  depth: number
  defaultExpanded: boolean
  worldNameById: Map<number, string>
  /** Only meaningful when pricing across a whole data center. */
  showWorld: boolean
}

/**
 * One item in the tree, with its ingredients nested beneath it.
 *
 * The quality badge, source world and job are shown only where they mean something: quality and
 * world only when the row's cost actually came from a purchase, job and level only for items that
 * have a recipe at all.
 *
 * Column widths here must mirror the header in {@link CraftCostTree} exactly - a change in one
 * needs the same change in the other, or the columns stop lining up.
 */
function NodeRow({ node, depth, defaultExpanded, worldNameById, showWorld }: NodeRowProps) {
  const hasChildren = node.ingredients.length > 0
  const [expanded, setExpanded] = React.useState(defaultExpanded)

  const unobtainable = node.effectiveCost === null
  const worldName = node.cheapestWorldId === null ? null : (worldNameById.get(node.cheapestWorldId) ?? null)
  const boughtHere = node.decision === 'BUY'

  return (
    <div>
      <div
        className={cn(
          'group flex items-center gap-3 border-b border-border px-3 py-2.5 text-sm last:border-b-0',
          'hover:bg-secondary/60',
          unobtainable && 'opacity-70',
        )}
        style={{ paddingLeft: `${depth * 1.25 + 0.75}rem` }}
      >
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          disabled={!hasChildren}
          aria-label={hasChildren ? (expanded ? 'Collapse ingredients' : 'Expand ingredients') : undefined}
          aria-expanded={hasChildren ? expanded : undefined}
          className={cn(
            'flex size-5 shrink-0 items-center justify-center rounded transition-colors',
            hasChildren ? 'hover:bg-border' : 'invisible',
          )}
        >
          <ChevronRight className={cn('size-4 transition-transform', expanded && 'rotate-90')} />
        </button>

        <span className="min-w-0 flex-1">
          <span className="block truncate font-medium" title={node.itemName}>
            {node.itemName}
          </span>
          <span className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-muted-foreground">
            {node.job && (
              <span className="flex items-center gap-1.5">
                <JobIcon craftType={node.job} />
                {jobName(node.job)}
                {node.level !== null && node.level > 0 && <span>· Lv {node.level}</span>}
              </span>
            )}
            {showWorld && boughtHere && worldName && (
              <span className="flex items-center gap-1">
                <MapPin className="size-3" />
                {worldName}
              </span>
            )}
          </span>
        </span>

        <span className="hidden w-9 shrink-0 sm:block">
          {node.buyQuality && boughtHere && <QualityBadge quality={node.buyQuality} />}
        </span>

        <span className="tabular w-12 shrink-0 text-right text-muted-foreground">
          ×{formatNumber(node.quantityNeeded)}
        </span>

        <span className="hidden w-28 shrink-0 sm:block">
          <DecisionBadge decision={node.decision} />
        </span>

        <span
          className={cn('tabular w-24 shrink-0 text-right font-medium', unobtainable && 'text-muted-foreground')}
          title={costTooltip(node, worldName)}
        >
          {formatGil(node.effectiveCost)}
        </span>
      </div>

      {hasChildren && expanded && (
        <div>
          {node.ingredients.map((child, index) => (
            <NodeRow
              key={`${child.itemXivapiId}-${index}`}
              node={child}
              depth={depth + 1}
              defaultExpanded={defaultExpanded}
              worldNameById={worldNameById}
              showWorld={showWorld}
            />
          ))}
        </div>
      )}
    </div>
  )
}

function costTooltip(node: CraftCostNode, worldName: string | null): string {
  const parts = [`Buy: ${formatGil(node.buyCost)} gil`, `Craft: ${formatGil(node.craftCost)} gil`]
  if (node.buyCostNq !== null) parts.push(`NQ: ${formatGil(node.buyCostNq)}`)
  if (node.buyCostHq !== null) parts.push(`HQ: ${formatGil(node.buyCostHq)}`)
  if (worldName) parts.push(`Cheapest on ${worldName}`)
  if (node.surplus > 0) parts.push(`Surplus: ${formatNumber(node.surplus)}`)
  if (node.craftsRequired > 0) parts.push(`Crafts: ${formatNumber(node.craftsRequired)}`)
  return parts.join(' · ')
}

/**
 * The recursive buy-versus-craft breakdown.
 *
 * Only the first two levels expand by default - a deep recipe expanded whole is hundreds of rows,
 * and the decision the user came for is almost always at the top.
 *
 * The header's column widths must mirror {@link NodeRow} exactly; changing one without the other
 * misaligns every row.
 *
 * @param showWorld reveal which world each purchase comes from, which is only meaningful when
 *                  pricing across a whole data center rather than a single world
 */
export function CraftCostTree({ root, showWorld = false }: { root: CraftCostNode; showWorld?: boolean }) {
  const { worldNameById } = useWorlds()

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="flex items-center gap-3 border-b border-border bg-secondary/50 px-3 py-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
        <span className="size-5 shrink-0" />
        <span className="flex-1">Item</span>
        <span className="hidden w-9 shrink-0 sm:block">Qual</span>
        <span className="w-12 shrink-0 text-right">Qty</span>
        <span className="hidden w-28 shrink-0 sm:block">Decision</span>
        <span className="w-24 shrink-0 text-right">Cost</span>
      </div>
      <NodeRow
        node={root}
        depth={0}
        defaultExpanded
        worldNameById={worldNameById}
        showWorld={showWorld}
      />
    </div>
  )
}

/**
 * Flattens the tree into just the items the plan says to buy, as a shopping list.
 *
 * A bought item's own ingredients are not collected - buying it means never making it.
 *
 * Quantities for the same item are combined across branches, but a null cost is contagious: one
 * unpriced entry makes the combined figure unknown rather than a partial sum that would read as
 * the true total.
 */
export function collectPurchases(
  root: CraftCostNode,
): { name: string; quantity: number; cost: number | null; worldId: number | null; quality: 'HQ' | 'NQ' | null }[] {
  const totals = new Map<
    string,
    { name: string; quantity: number; cost: number | null; worldId: number | null; quality: 'HQ' | 'NQ' | null }
  >()

  const walk = (node: CraftCostNode) => {
    if (node.decision === 'BUY') {
      const existing = totals.get(node.itemName)
      if (existing) {
        existing.quantity += node.quantityNeeded
        existing.cost =
          existing.cost === null || node.effectiveCost === null ? null : existing.cost + node.effectiveCost
      } else {
        totals.set(node.itemName, {
          name: node.itemName,
          quantity: node.quantityNeeded,
          cost: node.effectiveCost,
          worldId: node.cheapestWorldId,
          quality: node.buyQuality,
        })
      }
      return
    }
    node.ingredients.forEach(walk)
  }

  walk(root)
  return [...totals.values()].sort((a, b) => (b.cost ?? 0) - (a.cost ?? 0))
}
