import * as React from 'react'
import { Globe, TriangleAlert } from 'lucide-react'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { useWorlds } from '@/hooks/useWorlds'

function NotSyncedNotice({ message }: { message: string }) {
  return (
    <p className="flex items-start gap-2 rounded-md border border-warning/40 bg-warning/10 p-3 text-sm text-foreground">
      <TriangleAlert className="mt-0.5 size-4 shrink-0 text-warning" />
      <span>{message}</span>
    </p>
  )
}

interface WorldDataCenterPickerProps {
  dataCenter: string
  world: string
  onChange: (next: { dataCenter: string; world: string }) => void
  dataCenterError?: string
  worldError?: string
  disabled?: boolean
}

/**
 * Paired data-center and world selectors, for the forms where both are required.
 *
 * The world list is filtered to the chosen data center, and changing the data center drops a world
 * that no longer belongs to it. Both are deliberate: the backend rejects a mismatched pair with
 * WorldDataCenterMismatchException, so filtering here means the invalid combination cannot be
 * constructed at all rather than being discovered on submit.
 */
export function WorldDataCenterPicker({
  dataCenter,
  world,
  onChange,
  dataCenterError,
  worldError,
  disabled,
}: WorldDataCenterPickerProps) {
  const { dataCenters, worldsByDataCenter, loading, error } = useWorlds()

  const worldsForDc = React.useMemo(
    () => (dataCenter ? (worldsByDataCenter.get(dataCenter) ?? []) : []),
    [dataCenter, worldsByDataCenter],
  )

  if (loading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-9 w-full" />
        </div>
        <div className="space-y-2">
          <Skeleton className="h-4 w-16" />
          <Skeleton className="h-9 w-full" />
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <NotSyncedNotice
        message={
          error.isNotSynced
            ? 'No worlds have been synced yet. An administrator needs to run the world sync before you can pick one.'
            : error.message
        }
      />
    )
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <div className="space-y-2">
        <Label htmlFor="data-center">Data center</Label>
        <Select
          value={dataCenter || undefined}
          disabled={disabled}
          onValueChange={(next) => {
            const stillValid = (worldsByDataCenter.get(next) ?? []).some((w) => w.name === world)
            onChange({ dataCenter: next, world: stillValid ? world : '' })
          }}
        >
          <SelectTrigger id="data-center" aria-invalid={Boolean(dataCenterError)}>
            <SelectValue placeholder="Select a data center" />
          </SelectTrigger>
          <SelectContent>
            {groupByRegion(dataCenters).map(([region, group]) => (
              <SelectGroup key={region}>
                <SelectLabel>{region}</SelectLabel>
                {group.map((dc) => (
                  <SelectItem key={dc.name} value={dc.name}>
                    {dc.name}
                  </SelectItem>
                ))}
              </SelectGroup>
            ))}
          </SelectContent>
        </Select>
        {dataCenterError && <p className="text-xs text-destructive">{dataCenterError}</p>}
      </div>

      <div className="space-y-2">
        <Label htmlFor="world">Home world</Label>
        <Select
          value={world || undefined}
          disabled={disabled || !dataCenter}
          onValueChange={(next) => onChange({ dataCenter, world: next })}
        >
          <SelectTrigger id="world" aria-invalid={Boolean(worldError)}>
            <SelectValue placeholder={dataCenter ? 'Select a world' : 'Pick a data center first'} />
          </SelectTrigger>
          <SelectContent>
            {worldsForDc.map((w) => (
              <SelectItem key={w.name} value={w.name}>
                {w.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {worldError && <p className="text-xs text-destructive">{worldError}</p>}
      </div>
    </div>
  )
}

interface ScopeSelectProps {
  value: string
  onChange: (scope: string) => void
  disabled?: boolean
  className?: string
}

/**
 * Single "price against" selector, for endpoints whose `scope` takes either a world or a whole
 * data center. Data centers are listed first because cross-DC pricing is the cheaper answer more
 * often than a single world is.
 */
export function ScopeSelect({ value, onChange, disabled, className }: ScopeSelectProps) {
  const { dataCenters, worldsByDataCenter, loading, error } = useWorlds()

  if (loading) return <Skeleton className={className ?? 'h-9 w-56'} />
  if (error) {
    return (
      <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
        <TriangleAlert className="size-4 text-warning" />
        {error.isNotSynced ? 'Worlds not synced' : 'Worlds unavailable'}
      </span>
    )
  }

  return (
    <Select value={value || undefined} onValueChange={onChange} disabled={disabled}>
      <SelectTrigger className={className ?? 'w-56'} aria-label="Price against">
        <span className="flex min-w-0 items-center gap-2">
          <Globe className="size-4 shrink-0 text-muted-foreground" />
          <SelectValue placeholder="Price against…" />
        </span>
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel>Entire data center</SelectLabel>
          {dataCenters.map((dc) => (
            <SelectItem key={`dc-${dc.name}`} value={dc.name}>
              {dc.name}
            </SelectItem>
          ))}
        </SelectGroup>
        {dataCenters.map((dc) => {
          const worlds = worldsByDataCenter.get(dc.name) ?? []
          if (worlds.length === 0) return null
          return (
            <SelectGroup key={`worlds-${dc.name}`}>
              <SelectLabel>{dc.name} worlds</SelectLabel>
              {worlds.map((w) => (
                <SelectItem key={w.name} value={w.name}>
                  {w.name}
                </SelectItem>
              ))}
            </SelectGroup>
          )
        })}
      </SelectContent>
    </Select>
  )
}

function groupByRegion<T extends { region: string; name: string }>(items: T[]): [string, T[]][] {
  const grouped = new Map<string, T[]>()
  for (const item of items) {
    const bucket = grouped.get(item.region)
    if (bucket) bucket.push(item)
    else grouped.set(item.region, [item])
  }
  return [...grouped.entries()]
}
