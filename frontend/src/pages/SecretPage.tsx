import * as React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { canSeeSecretPage } from '@/lib/secretAccess'

/** Served from frontend/public, so no import is needed and the build works before it exists. */
const IMAGE_SRC = '/secret-image.jpg'

/**
 * A single image, visible to one account.
 *
 * Rendered inside ProtectedRoute, so the session has already resolved by the time this runs and a
 * page refresh cannot bounce the right user out before /users/me has answered.
 *
 * The gate is presentational only. The image is a static file and stays reachable by anyone who
 * knows or guesses its URL - the route is hidden, the file is not. That is fine for something
 * personal but not for anything that actually needs protecting, which would have to be served
 * from an authenticated endpoint instead.
 */
export function SecretPage() {
  const { user } = useAuth()
  const [failed, setFailed] = React.useState(false)

  if (!canSeeSecretPage(user)) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="flex flex-col items-center gap-4 py-8">
      {failed ? (
        <div className="max-w-md rounded-xl border border-dashed border-border p-8 text-center">
          <p className="font-medium">No image yet</p>
          <p className="mt-1 text-sm text-muted-foreground">
            Drop a file at <code className="font-mono">frontend/public{IMAGE_SRC}</code>, or point{' '}
            <code className="font-mono">IMAGE_SRC</code> at whatever you name it.
          </p>
        </div>
      ) : (
        <img
          src={IMAGE_SRC}
          alt=""
          onError={() => setFailed(true)}
          className="max-h-[80dvh] w-auto max-w-full rounded-xl"
        />
      )}
    </div>
  )
}
