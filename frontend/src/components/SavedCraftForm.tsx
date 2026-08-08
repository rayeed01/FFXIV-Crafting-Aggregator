import * as React from 'react'
import { ApiError } from '@/lib/api'
import { useWorlds } from '@/hooks/useWorlds'
import { Input, Textarea } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

export interface SavedCraftFormValues {
  title: string
  notes: string
  dataCenter: string
  /** Empty means price across the whole data center - the backend allows a null world. */
  world: string
}

export interface SavedCraftForm {
  values: SavedCraftFormValues
  setValue: (patch: Partial<SavedCraftFormValues>) => void
  error: ApiError | null
  submitting: boolean
  submit: (action: (values: SavedCraftFormValues) => Promise<void>) => Promise<void>
}

/**
 * Shared state for the create and edit dialogs.
 *
 * Resetting keys off `open` rather than mount, because a dialog stays mounted between openings -
 * without it, reopening would show whatever was typed the previous time.
 */
export function useSavedCraftForm({
  open,
  initial,
}: {
  open: boolean
  initial: SavedCraftFormValues
}): SavedCraftForm {
  const [values, setValues] = React.useState<SavedCraftFormValues>(initial)
  const [error, setError] = React.useState<ApiError | null>(null)
  const [submitting, setSubmitting] = React.useState(false)

  // Depend on the fields, not the object: callers build `initial` inline, so a new identity
  // arrives every render and would reset the form on each keystroke.
  const { title, notes, dataCenter, world } = initial
  React.useEffect(() => {
    if (open) {
      setValues({ title, notes, dataCenter, world })
      setError(null)
    }
  }, [open, title, notes, dataCenter, world])

  const setValue = React.useCallback(
    (patch: Partial<SavedCraftFormValues>) => setValues((prev) => ({ ...prev, ...patch })),
    [],
  )

  const submit = React.useCallback(
    async (action: (values: SavedCraftFormValues) => Promise<void>) => {
      setSubmitting(true)
      setError(null)
      try {
        await action(values)
      } catch (err) {
        setError(err instanceof ApiError ? err : new ApiError(0, 'Could not save this list.'))
      } finally {
        setSubmitting(false)
      }
    },
    [values],
  )

  return { values, setValue, error, submitting, submit }
}

export function SavedCraftFormFields({ form }: { form: SavedCraftForm }) {
  const { dataCenters, worldsByDataCenter, loading } = useWorlds()
  const { values, setValue, error, submitting } = form

  const worldsForDc = values.dataCenter ? (worldsByDataCenter.get(values.dataCenter) ?? []) : []

  return (
    <>
      {error && (
        <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm">
          {error.message}
        </p>
      )}

      <div className="space-y-2">
        <Label htmlFor="list-title">Title</Label>
        <Input
          id="list-title"
          value={values.title}
          onChange={(e) => setValue({ title: e.target.value })}
          required
          autoFocus
          aria-invalid={Boolean(error?.fieldError('title'))}
        />
        {error?.fieldError('title') && (
          <p className="text-xs text-destructive">{error.fieldError('title')}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="list-notes">Notes (optional)</Label>
        <Textarea
          id="list-notes"
          value={values.notes}
          onChange={(e) => setValue({ notes: e.target.value })}
        />
      </div>

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2">
          <Skeleton className="h-9 w-full" />
          <Skeleton className="h-9 w-full" />
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="list-dc">Data center</Label>
            <Select
              value={values.dataCenter || undefined}
              disabled={submitting}
              onValueChange={(next) => setValue({ dataCenter: next, world: '' })}
            >
              <SelectTrigger id="list-dc" aria-invalid={Boolean(error?.fieldError('dataCenter'))}>
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
            {error?.fieldError('dataCenter') && (
              <p className="text-xs text-destructive">{error.fieldError('dataCenter')}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="list-world">World</Label>
            <Select
              value={values.world || '__ALL__'}
              disabled={submitting || !values.dataCenter}
              onValueChange={(next) => setValue({ world: next === '__ALL__' ? '' : next })}
            >
              <SelectTrigger id="list-world">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  {/* Radix forbids an empty-string value, so a sentinel stands in for "any". */}
                  <SelectItem value="__ALL__">Any world (whole DC)</SelectItem>
                </SelectGroup>
                {worldsForDc.length > 0 && (
                  <SelectGroup>
                    <SelectLabel>{values.dataCenter} worlds</SelectLabel>
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
      )}
    </>
  )
}
