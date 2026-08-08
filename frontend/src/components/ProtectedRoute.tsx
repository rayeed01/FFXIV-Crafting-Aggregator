import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { Skeleton } from '@/components/ui/skeleton'

function BootSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-8 w-56" />
      <Skeleton className="h-32 w-full" />
    </div>
  )
}

/**
 * Gate for authenticated routes.
 *
 * Waiting on `initialising` matters: without it a page refresh renders before the stored token
 * has been validated, bouncing a signed-in user to the login screen every reload.
 */
export function ProtectedRoute({ requireAdmin = false }: { requireAdmin?: boolean }) {
  const { isAuthenticated, isAdmin, initialising } = useAuth()
  const location = useLocation()

  if (initialising) return <BootSkeleton />

  if (!isAuthenticated) {
    // Remember where they were headed so login can return them there.
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (requireAdmin && !isAdmin) return <Navigate to="/" replace />

  return <Outlet />
}
