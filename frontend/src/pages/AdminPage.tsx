import * as React from 'react'
import { Database, Globe, Loader2, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError, api } from '@/lib/api'
import { invalidateWorldCache } from '@/hooks/useWorlds'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ErrorState, PageHeader } from '@/components/states'
import { formatDateTime, formatNumber, formatRelative } from '@/lib/format'
import type { GameServerSyncResult, SyncStatus } from '@/types/api'

/** How often to re-poll while a bulk recipe sync is running. */
const POLL_INTERVAL_MS = 3000

export function AdminPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Admin"
        description="Populate the local database from XIVAPI and Universalis."
      />
      <div className="grid gap-6 lg:grid-cols-2">
        <WorldSyncCard />
        <RecipeSyncCard />
      </div>
    </div>
  )
}

/**
 * Triggers the Universalis world and data-centre sync.
 *
 * Invalidates the shared world cache on success, so selectors elsewhere pick up newly added worlds
 * without a page reload.
 */
function WorldSyncCard() {
  const [running, setRunning] = React.useState(false)
  const [result, setResult] = React.useState<GameServerSyncResult | null>(null)
  const [error, setError] = React.useState<ApiError | null>(null)

  async function sync() {
    setRunning(true)
    setError(null)
    try {
      const outcome = await api.admin.syncWorlds()
      setResult(outcome)
      invalidateWorldCache()
      toast.success(`Synced ${outcome.worldsSynced} worlds across ${outcome.dataCentersSynced} data centers`)
    } catch (err) {
      const apiError = err instanceof ApiError ? err : new ApiError(0, 'Could not sync worlds.')
      setError(apiError)
      toast.error(apiError.message)
    } finally {
      setRunning(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Globe className="size-4" /> Worlds and data centers
        </CardTitle>
        <CardDescription>
          Pulls the server list from Universalis. Run this first — world validation and every
          selector depend on it.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {error && <ErrorState error={error} />}

        {result && (
          <dl className="grid grid-cols-3 gap-3 rounded-lg border border-border bg-secondary/40 p-3 text-center">
            <div>
              <dt className="text-xs text-muted-foreground">Data centers</dt>
              <dd className="tabular text-lg font-semibold">{formatNumber(result.dataCentersSynced)}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Worlds</dt>
              <dd className="tabular text-lg font-semibold">{formatNumber(result.worldsSynced)}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Skipped</dt>
              <dd className="tabular text-lg font-semibold">{formatNumber(result.worldsSkipped)}</dd>
            </div>
          </dl>
        )}

        <Button onClick={sync} disabled={running}>
          {running ? <Loader2 className="animate-spin" /> : <RefreshCw />}
          Sync worlds
        </Button>
      </CardContent>
    </Card>
  )
}

/**
 * Starts and monitors the XIVAPI bulk recipe import.
 *
 * Status is polled only while a sync is actually running: the endpoint is otherwise static, and a
 * permanent timer would keep hitting the API from an idle tab.
 *
 * A 409 on start is SyncAlreadyRunningException rather than a real failure, so the view is
 * refreshed to pick up the run that is already in flight.
 */
function RecipeSyncCard() {
  const [status, setStatus] = React.useState<SyncStatus | null>(null)
  const [error, setError] = React.useState<ApiError | null>(null)
  const [starting, setStarting] = React.useState(false)

  const refresh = React.useCallback(async (signal?: AbortSignal) => {
    try {
      const next = await api.admin.syncStatus(signal)
      setStatus(next)
      setError(null)
      return next
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') return null
      setError(err instanceof ApiError ? err : new ApiError(0, 'Could not read sync status.'))
      return null
    }
  }, [])

  React.useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  React.useEffect(() => {
    if (!status?.running) return
    const timer = setInterval(() => void refresh(), POLL_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [status?.running, refresh])

  async function start() {
    setStarting(true)
    try {
      setStatus(await api.admin.startRecipeSync())
      toast.success('Recipe sync started')
    } catch (err) {
      const apiError = err instanceof ApiError ? err : new ApiError(0, 'Could not start the sync.')
      if (apiError.status === 409) void refresh()
      toast.error(apiError.message)
    } finally {
      setStarting(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Database className="size-4" /> Items and recipes
        </CardTitle>
        <CardDescription>
          Bulk-imports the recipe catalogue from XIVAPI. Runs in the background and can take a
          while.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {error && <ErrorState error={error} onRetry={() => void refresh()} />}

        {status && (
          <div className="space-y-3 rounded-lg border border-border bg-secondary/40 p-4">
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm text-muted-foreground">Status</span>
              {status.running ? (
                <Badge variant="craft">
                  <Loader2 className="size-3 animate-spin" /> Running
                </Badge>
              ) : (
                <Badge variant="success">Idle</Badge>
              )}
            </div>
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm text-muted-foreground">Recipes synced</span>
              <span className="tabular font-semibold">{formatNumber(status.syncedCount)}</span>
            </div>
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm text-muted-foreground">Started</span>
              <span className="text-sm" title={formatDateTime(status.startedAt)}>
                {status.startedAt ? formatRelative(status.startedAt) : '—'}
              </span>
            </div>
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm text-muted-foreground">Finished</span>
              <span className="text-sm" title={formatDateTime(status.finishedAt)}>
                {status.finishedAt ? formatRelative(status.finishedAt) : '—'}
              </span>
            </div>
          </div>
        )}

        <div className="flex gap-2">
          <Button onClick={start} disabled={starting || status?.running}>
            {starting ? <Loader2 className="animate-spin" /> : <RefreshCw />}
            {status?.running ? 'Sync in progress' : 'Start recipe sync'}
          </Button>
          <Button variant="outline" onClick={() => void refresh()}>
            Refresh
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
