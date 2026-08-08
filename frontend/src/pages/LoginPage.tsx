import * as React from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { ApiError } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/ui/password-input'
import { Label } from '@/components/ui/label'

export function LoginPage() {
  const { login, isAuthenticated, initialising } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [username, setUsername] = React.useState('')
  const [password, setPassword] = React.useState('')
  const [error, setError] = React.useState<ApiError | null>(null)
  const [submitting, setSubmitting] = React.useState(false)

  const redirectTo = (location.state as { from?: Location } | null)?.from?.pathname ?? '/search'

  if (!initialising && isAuthenticated) return <Navigate to={redirectTo} replace />

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await login({ username, password })
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Could not reach the server.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md py-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl">Welcome back</CardTitle>
          <CardDescription>Sign in to price your crafts and manage saved lists.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {error && (
              <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm">
                {error.message}
              </p>
            )}

            <div className="space-y-2">
              <Label htmlFor="username">Username</Label>
              <Input
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                required
                autoFocus
                aria-invalid={Boolean(error?.fieldError('username'))}
              />
              {error?.fieldError('username') && (
                <p className="text-xs text-destructive">{error.fieldError('username')}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <PasswordInput
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
                aria-invalid={Boolean(error?.fieldError('password'))}
              />
              {error?.fieldError('password') && (
                <p className="text-xs text-destructive">{error.fieldError('password')}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting && <Loader2 className="animate-spin" />}
              Sign in
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              No account?{' '}
              <Link to="/register" className="font-medium text-primary hover:underline">
                Create one
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
