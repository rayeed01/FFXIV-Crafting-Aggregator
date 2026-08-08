import * as React from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { ApiError } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/ui/password-input'
import { Label } from '@/components/ui/label'
import { WorldDataCenterPicker } from '@/components/WorldPicker'

/**
 * Account creation.
 *
 * The world, data centre, password length and confirmation are all checked client-side first.
 * These are server-side rules too, but a round trip to learn about them also clears the password
 * fields, which is a poor way to find out about a typo.
 *
 * The confirmation mismatch is only reported once something has actually been typed in that
 * field, so the form does not scold the user before they have finished.
 */
export function RegisterPage() {
  const { register, isAuthenticated, initialising } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = React.useState({
    username: '',
    email: '',
    password: '',
    defaultDataCenter: '',
    defaultWorld: '',
  })
  const [confirmPassword, setConfirmPassword] = React.useState('')
  const [error, setError] = React.useState<ApiError | null>(null)
  const [localError, setLocalError] = React.useState<string | null>(null)
  const [submitting, setSubmitting] = React.useState(false)

  if (!initialising && isAuthenticated) return <Navigate to="/search" replace />

  const update = (patch: Partial<typeof form>) => setForm((prev) => ({ ...prev, ...patch }))
  const mismatch = confirmPassword.length > 0 && confirmPassword !== form.password

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setError(null)
    setLocalError(null)

    if (!form.defaultDataCenter || !form.defaultWorld) {
      setLocalError('Choose both a data center and a home world.')
      return
    }
    if (form.password.length < 8) {
      setLocalError('Password must be at least 8 characters.')
      return
    }
    if (form.password !== confirmPassword) {
      setLocalError('The two passwords do not match.')
      return
    }

    setSubmitting(true)
    try {
      await register(form)
      navigate('/search', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Could not reach the server.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl py-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl">Create an account</CardTitle>
          <CardDescription>
            Your home world sets the default market to price against. You can change it later.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {(error || localError) && (
              <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm">
                {localError ?? error?.message}
              </p>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="username">Username</Label>
                <Input
                  id="username"
                  value={form.username}
                  onChange={(e) => update({ username: e.target.value })}
                  autoComplete="username"
                  required
                  autoFocus
                  aria-invalid={Boolean(error?.fieldError('username'))}
                />
                <p className="text-xs text-muted-foreground">
                  {error?.fieldError('username') ?? '3–20 characters, letters, numbers and spaces.'}
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={form.email}
                  onChange={(e) => update({ email: e.target.value })}
                  autoComplete="email"
                  required
                  aria-invalid={Boolean(error?.fieldError('email'))}
                />
                {error?.fieldError('email') && (
                  <p className="text-xs text-destructive">{error.fieldError('email')}</p>
                )}
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <PasswordInput
                  id="password"
                  value={form.password}
                  onChange={(e) => update({ password: e.target.value })}
                  autoComplete="new-password"
                  required
                  aria-invalid={Boolean(error?.fieldError('password'))}
                />
                <p className="text-xs text-muted-foreground">
                  {error?.fieldError('password') ?? 'At least 8 characters.'}
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirm-password">Confirm password</Label>
                <PasswordInput
                  id="confirm-password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                  aria-invalid={mismatch}
                />
                {mismatch ? (
                  <p className="text-xs text-destructive">Passwords do not match.</p>
                ) : (
                  <p className="text-xs text-muted-foreground">Type it again to be sure.</p>
                )}
              </div>
            </div>

            <WorldDataCenterPicker
              dataCenter={form.defaultDataCenter}
              world={form.defaultWorld}
              onChange={({ dataCenter, world }) =>
                update({ defaultDataCenter: dataCenter, defaultWorld: world })
              }
              dataCenterError={error?.fieldError('defaultDataCenter')}
              worldError={error?.fieldError('defaultWorld')}
              disabled={submitting}
            />

            <Button type="submit" className="w-full" disabled={submitting || mismatch}>
              {submitting && <Loader2 className="animate-spin" />}
              Create account
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              Already registered?{' '}
              <Link to="/login" className="font-medium text-primary hover:underline">
                Sign in
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
