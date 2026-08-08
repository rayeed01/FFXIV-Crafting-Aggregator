import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { Skeleton } from '@/components/ui/skeleton'

/** Placeholder shown while the stored token is being validated. */
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
 * has been validated, bouncing a signed-in user to the login screen on every reload.
 *
 * The attempted location is passed along in router state so the login page can return the user
 * to where they were headed rather than dropping them on the default page.
 *
 * @param requireAdmin also require the ADMIN role, redirecting others to the home page
 */
export function ProtectedRoute({ requireAdmin = false }: { requireAdmin?: boolean }) {
  const { isAuthenticated, isAdmin, initialising } = useAuth()
  const location = useLocation()

  if (initialising) return <BootSkeleton />

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (requireAdmin && !isAdmin) return <Navigate to="/" replace />

  return <Outlet />
}
