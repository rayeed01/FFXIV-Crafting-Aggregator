import * as React from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Calculator, MapPin, Search as SearchIcon, TrendingDown, TrendingUp } from 'lucide-react'
import { api } from '@/lib/api'
import { useAsync, useDebounced } from '@/hooks/useAsync'
import { useAuth } from '@/context/AuthContext'
import { useWorlds } from '@/hooks/useWorlds'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ItemIcon } from '@/components/ItemIcon'
import { CraftCostTree, DecisionBadge, QualityBadge, collectPurchases } from '@/components/CraftCostTree'
import { EmptyState, ErrorState, PageHeader } from '@/components/states'
import { formatGil, formatNumber, percentDelta } from '@/lib/format'
import { cn } from '@/lib/utils'
import type { CraftCostNode, Quality } from '@/types/api'

const QUALITY_OPTIONS: { value: Quality; label: string; hint: string }[] = [
  { value: 'CHEAPEST', label: 'Cheapest', hint: 'Whichever of NQ or HQ is listed lower' },
  { value: 'HQ', label: 'HQ only', hint: 'Price the item as high quality' },
  { value: 'NQ', label: 'NQ only', hint: 'Price the item as normal quality' },
]

/**
 * Buy-versus-craft calculator for a single item.
 *
 * Data centre and world are separate controls, and an empty world means "price across the whole
 * data centre" - frequently the cheaper answer, and the case where showing each purchase's source
 * world is meaningful. The backend's `scope` takes either, so a chosen world wins and the data
 * centre is the fallback.
 *
 * Every control is mirrored into the URL so a result is shareable and survives a reload. The
 * signed-in default data centre is adopted only when nothing was supplied, so it never clobbers an
 * explicit choice from a shared link.
 *
 * Public: signed-out visitors price one item at a time, with lists reserved for accounts.
 */
export function CraftCostPage() {
  const { itemXivapiId } = useParams<{ itemXivapiId?: string }>()
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const { user, isAuthenticated } = useAuth()
  const { worldNameById, worldsByDataCenter, dataCenters, loading: worldsLoading } = useWorlds()

  const [dataCenter, setDataCenter] = React.useState(
    () => params.get('dataCenter') ?? user?.defaultDataCenter ?? '',
  )
  const [world, setWorld] = React.useState(() => params.get('world') ?? '')
  const [quantity, setQuantity] = React.useState(() => Number(params.get('quantity')) || 1)
  const [quality, setQuality] = React.useState<Quality>(
    () => (params.get('quality') as Quality) || 'CHEAPEST',
  )

  React.useEffect(() => {
    if (!dataCenter && user?.defaultDataCenter) setDataCenter(user.defaultDataCenter)
  }, [dataCenter, user?.defaultDataCenter])

  React.useEffect(() => {
    const next = new URLSearchParams()
    if (dataCenter) next.set('dataCenter', dataCenter)
    if (world) next.set('world', world)
    if (quantity !== 1) next.set('quantity', String(quantity))
    if (quality !== 'CHEAPEST') next.set('quality', quality)
    setParams(next, { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dataCenter, world, quantity, quality])

  const scope = world || dataCenter
  const pricingWholeDataCenter = !world

  const itemId = itemXivapiId ? Number(itemXivapiId) : null
  const ready = itemId !== null && Number.isFinite(itemId) && scope !== '' && quantity >= 1

  const { data, error, loading, reload } = useAsync(
    (signal) => api.craftCost.calculate(itemId!, quantity, scope, quality, signal),
    [itemId, quantity, scope, quality],
    ready,
  )

  const worldsForDc = dataCenter ? (worldsByDataCenter.get(dataCenter) ?? []) : []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Craft Cost"
        description="Compare buying outright against crafting from components, ingredient by ingredient."
      />

      {!isAuthenticated && (
        <p className="rounded-lg border border-border bg-secondary/50 p-3 text-sm text-muted-foreground">
          You're browsing signed out — you can price one item at a time.{' '}
          <Link to="/register" className="font-medium text-primary hover:underline">
            Create an account
          </Link>{' '}
          to save lists and price many recipes together.
        </p>
      )}

      <Card>
        <CardContent className="grid gap-4 p-5 lg:grid-cols-2">
          <div className="space-y-2 lg:col-span-2">
            <Label>Item</Label>
            <ItemPicker onSelect={(id) => navigate(`/craft-cost/${id}?${params.toString()}`)} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="dc">Data center</Label>
              <Select
                value={dataCenter || undefined}
                disabled={worldsLoading}
                onValueChange={(next) => {
                  setDataCenter(next)
                  setWorld('') // a world from the old DC would be an invalid pair
                }}
              >
                <SelectTrigger id="dc">
                  <SelectValue placeholder="Select a data center" />
                </SelectTrigger>
                <SelectContent>
                  {dataCenters.map((dc) => (
                    <SelectItem key={dc.name} value={dc.name}>
                      {dc.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="world">World</Label>
              <Select
                value={world || '__ALL__'}
                disabled={worldsLoading || !dataCenter}
                onValueChange={(next) => setWorld(next === '__ALL__' ? '' : next)}
              >
                <SelectTrigger id="world">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    <SelectItem value="__ALL__">Any world (whole DC)</SelectItem>
                  </SelectGroup>
                  {worldsForDc.length > 0 && (
                    <SelectGroup>
                      <SelectLabel>{dataCenter} worlds</SelectLabel>
                      {worldsForDc.map((w) => (
                        <SelectItem key={w.name} value={w.name}>
                          {w.name}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  )}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="quantity">Quantity</Label>
              <Input
                id="quantity"
                type="number"
                min={1}
                max={999}
                value={quantity}
                onChange={(e) => setQuantity(Math.max(1, Math.min(999, Number(e.target.value) || 1)))}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="quality">Quality</Label>
              <Select value={quality} onValueChange={(next) => setQuality(next as Quality)}>
                <SelectTrigger id="quality">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {QUALITY_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                {QUALITY_OPTIONS.find((o) => o.value === quality)?.hint} — applies to this item,
                not its ingredients.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {itemId === null && (
        <EmptyState
          icon={Calculator}
          title="Pick an item to price"
          description="Search above, or start from any item in Browse."
        />
      )}

      {ready && loading && (
        <div className="space-y-3">
          <div className="grid gap-3 sm:grid-cols-3">
            <Skeleton className="h-24" />
            <Skeleton className="h-24" />
            <Skeleton className="h-24" />
          </div>
          <Skeleton className="h-64 w-full" />
        </div>
      )}

      {error && <ErrorState error={error} onRetry={reload} />}

      {!loading && !error && data && (
        <>
          <CostSummary
            node={data}
            worldNameById={worldNameById}
            showWorld={pricingWholeDataCenter}
            requestedQuality={quality}
          />
          <section className="space-y-3">
            <h2 className="text-lg font-semibold tracking-tight">Breakdown</h2>
            <CraftCostTree root={data} showWorld={pricingWholeDataCenter} />
          </section>
          <ShoppingList root={data} worldNameById={worldNameById} showWorld={pricingWholeDataCenter} />
        </>
      )}
    </div>
  )
}

/**
 * Headline craft, buy and recommendation figures for the root item.
 *
 * When the requested quality has no listing the backend falls back to the cheaper one, and that
 * substitution is called out rather than silently showing a price for a quality the user did not
 * ask for.
 */
function CostSummary({
  node,
  worldNameById,
  showWorld,
  requestedQuality,
}: {
  node: CraftCostNode
  worldNameById: Map<number, string>
  showWorld: boolean
  requestedQuality: Quality
}) {
  const delta = percentDelta(node.craftCost, node.buyCost)
  const cheaperToCraft = delta !== null && delta < 0
  const worldName = node.cheapestWorldId === null ? null : worldNameById.get(node.cheapestWorldId)

  const fellBack =
    requestedQuality !== 'CHEAPEST' && node.buyQuality !== null && node.buyQuality !== requestedQuality

  return (
    <div className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Craft cost</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="tabular text-2xl font-semibold">{formatGil(node.craftCost)}</p>
            {delta !== null && (
              <p
                className={cn(
                  'mt-1 flex items-center gap-1 text-xs font-medium',
                  cheaperToCraft ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400',
                )}
              >
                {cheaperToCraft ? <TrendingDown className="size-3" /> : <TrendingUp className="size-3" />}
                {Math.abs(delta).toFixed(1)}% {cheaperToCraft ? 'cheaper than buying' : 'more than buying'}
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              Market buy
              {node.buyQuality && <QualityBadge quality={node.buyQuality} />}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="tabular text-2xl font-semibold">{formatGil(node.buyCost)}</p>
            {node.buyCost === null ? (
              <p className="mt-1 text-xs text-muted-foreground">Not listed on the market board</p>
            ) : (
              <div className="mt-1 space-y-0.5 text-xs text-muted-foreground">
                <p>
                  NQ {formatGil(node.buyCostNq)} · HQ {formatGil(node.buyCostHq)}
                </p>
                {showWorld && worldName && (
                  <p className="flex items-center gap-1">
                    <MapPin className="size-3" />
                    Cheapest on {worldName}
                  </p>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Recommendation</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <DecisionBadge decision={node.decision} />
            <p className="tabular text-lg font-semibold">{formatGil(node.effectiveCost)}</p>
            {node.surplus > 0 && (
              <p className="text-xs text-muted-foreground">
                Leaves {formatNumber(node.surplus)} surplus over {formatNumber(node.craftsRequired)} craft
                {node.craftsRequired === 1 ? '' : 's'}
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      {fellBack && (
        <p className="rounded-md border border-warning/40 bg-warning/10 p-3 text-sm">
          No {requestedQuality} listing for this item right now — showing the {node.buyQuality} price
          instead.
        </p>
      )}
    </div>
  )
}

function ShoppingList({
  root,
  worldNameById,
  showWorld,
}: {
  root: CraftCostNode
  worldNameById: Map<number, string>
  showWorld: boolean
}) {
  const purchases = React.useMemo(() => collectPurchases(root), [root])
  if (purchases.length === 0) return null

  const total = purchases.reduce<number | null>(
    (sum, p) => (sum === null || p.cost === null ? null : sum + p.cost),
    0,
  )

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold tracking-tight">Shopping list</h2>
      <p className="text-sm text-muted-foreground">
        Everything the plan says to buy, with duplicate ingredients across branches combined.
      </p>
      <div className="overflow-hidden rounded-xl border border-border bg-card">
        <ul className="divide-y divide-border">
          {purchases.map((purchase) => {
            const worldName = purchase.worldId === null ? null : worldNameById.get(purchase.worldId)
            return (
              <li key={purchase.name} className="flex items-center gap-3 px-4 py-2.5 text-sm">
                <span className="min-w-0 flex-1">
                  <span className="block truncate">{purchase.name}</span>
                  {showWorld && worldName && (
                    <span className="flex items-center gap-1 text-xs text-muted-foreground">
                      <MapPin className="size-3" />
                      {worldName}
                    </span>
                  )}
                </span>
                {purchase.quality && <QualityBadge quality={purchase.quality} />}
                <span className="tabular shrink-0 text-muted-foreground">×{formatNumber(purchase.quantity)}</span>
                <span className="tabular w-24 shrink-0 text-right font-medium">{formatGil(purchase.cost)}</span>
              </li>
            )
          })}
        </ul>
        <div className="flex items-center gap-3 border-t border-border bg-secondary/50 px-4 py-2.5 text-sm font-semibold">
          <span className="flex-1">Total</span>
          <span className="tabular w-24 text-right">{formatGil(total)}</span>
        </div>
      </div>
    </section>
  )
}

/**
 * Inline item search that resolves a name to the xivapiId the craft-cost endpoint expects.
 *
 * Closing on blur is deferred briefly so that a click on a result registers before the list
 * unmounts beneath the pointer.
 */
function ItemPicker({ onSelect }: { onSelect: (id: number) => void }) {
  const [query, setQuery] = React.useState('')
  const debounced = useDebounced(query, 250)
  const [open, setOpen] = React.useState(false)

  const { data, loading } = useAsync(
    (signal) => api.items.search(debounced, signal),
    [debounced],
    debounced.trim().length > 0,
  )

  const results = (data ?? []).slice(0, 10)

  return (
    <div className="relative">
      <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        value={query}
        onChange={(e) => {
          setQuery(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        placeholder="Search items…"
        className="pl-9"
      />

      {open && debounced.trim().length > 0 && (
        <div className="absolute z-30 mt-1 w-full overflow-hidden rounded-md border border-border bg-popover shadow-md">
          {loading && <p className="p-3 text-sm text-muted-foreground">Searching…</p>}
          {!loading && results.length === 0 && (
            <p className="p-3 text-sm text-muted-foreground">No items matched.</p>
          )}
          <ul className="max-h-72 overflow-y-auto">
            {results.map((item) => (
              <li key={item.id}>
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => {
                    onSelect(item.xivapiId)
                    setQuery('')
                    setOpen(false)
                  }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-accent hover:text-accent-foreground"
                >
                  <ItemIcon src={item.iconUrl} alt={item.name} className="size-6" />
                  <span className="min-w-0 flex-1 truncate">{item.name}</span>
                  {item.canBeCrafted && <Badge variant="craft">Craftable</Badge>}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
