import type { LucideIcon } from 'lucide-react'
import { CircleAlert, Inbox, RefreshCw, TriangleAlert } from 'lucide-react'
import type { ApiError } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string
  description?: string
  actions?: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div className="min-w-0 space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}

/**
 * Failure display. A 503 is called out separately because it is the one error with a specific
 * remedy - an admin has not run the sync - and "Something went wrong" would send the user
 * looking for a mistake they did not make.
 */
export function ErrorState({
  error,
  onRetry,
  className,
}: {
  error: ApiError
  onRetry?: () => void
  className?: string
}) {
  const notSynced = error.isNotSynced

  return (
    <div
      className={cn(
        'flex flex-col items-center gap-3 rounded-xl border p-8 text-center',
        notSynced ? 'border-warning/40 bg-warning/10' : 'border-destructive/40 bg-destructive/5',
        className,
      )}
    >
      {notSynced ? (
        <TriangleAlert className="size-6 text-warning" />
      ) : (
        <CircleAlert className="size-6 text-destructive" />
      )}
      <div className="space-y-1">
        <p className="font-medium">{notSynced ? 'Game data not synced yet' : 'Something went wrong'}</p>
        <p className="max-w-prose text-sm text-muted-foreground">{error.message}</p>
      </div>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCw /> Try again
        </Button>
      )}
    </div>
  )
}

export function EmptyState({
  icon: Icon = Inbox,
  title,
  description,
  action,
}: {
  icon?: LucideIcon
  title: string
  description?: string
  action?: React.ReactNode
}) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-border p-10 text-center">
      <Icon className="size-6 text-muted-foreground" />
      <div className="space-y-1">
        <p className="font-medium">{title}</p>
        {description && <p className="max-w-prose text-sm text-muted-foreground">{description}</p>}
      </div>
      {action}
    </div>
  )
}
