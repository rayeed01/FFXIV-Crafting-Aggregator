import * as React from 'react'
import { Link } from 'react-router-dom'
import { Bookmark, Calendar, Check, Loader2, LogOut, Mail, Moon, ShieldCheck, Sun, User } from 'lucide-react'
import { toast } from 'sonner'
import { ApiError, api } from '@/lib/api'
import { useAuth } from '@/context/AuthContext'
import { useTheme } from '@/context/ThemeContext'
import { useAsync } from '@/hooks/useAsync'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { WorldDataCenterPicker } from '@/components/WorldPicker'
import { PageHeader } from '@/components/states'
import { formatDate, formatNumber } from '@/lib/format'

export function ProfilePage() {
  const { user, setUser, logout, isAdmin } = useAuth()

  if (!user) return null

  return (
    <div className="space-y-6">
      <PageHeader title="Profile" description="Your account details and default market." />

      <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <div className="space-y-6">
          <AccountCard user={user} isAdmin={isAdmin} />
          <PreferencesCard user={user} onSaved={setUser} />
          <AppearanceCard />
        </div>

        <div className="space-y-6">
          <ActivityCard />
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Session</CardTitle>
              <CardDescription>Signing out clears your token from this browser.</CardDescription>
            </CardHeader>
            <CardContent>
              <Button variant="outline" onClick={logout} className="w-full">
                <LogOut /> Sign out
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}

function AccountCard({ user, isAdmin }: { user: import('@/types/api').UserDto; isAdmin: boolean }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Account</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center gap-4">
          <span className="grid size-14 shrink-0 place-items-center rounded-full bg-primary text-xl font-semibold text-primary-foreground">
            {user.username.charAt(0).toUpperCase()}
          </span>
          <div className="min-w-0">
            <p className="truncate text-lg font-semibold">{user.username}</p>
            <div className="mt-1 flex flex-wrap items-center gap-2">
              <Badge variant={isAdmin ? 'craft' : 'secondary'}>
                {isAdmin && <ShieldCheck className="size-3" />}
                {user.role}
              </Badge>
              {isAdmin && (
                <Button variant="link" size="sm" asChild className="h-auto p-0">
                  <Link to="/admin">Admin tools</Link>
                </Button>
              )}
            </div>
          </div>
        </div>

        <dl className="grid gap-3 border-t border-border pt-4 text-sm sm:grid-cols-2">
          <div className="space-y-1">
            <dt className="flex items-center gap-1.5 text-muted-foreground">
              <Mail className="size-3.5" /> Email
            </dt>
            <dd className="truncate font-medium">{user.email}</dd>
          </div>
          <div className="space-y-1">
            <dt className="flex items-center gap-1.5 text-muted-foreground">
              <Calendar className="size-3.5" /> Member since
            </dt>
            <dd className="font-medium">{formatDate(user.createdAt)}</dd>
          </div>
          <div className="space-y-1">
            <dt className="flex items-center gap-1.5 text-muted-foreground">
              <User className="size-3.5" /> User ID
            </dt>
            <dd className="truncate font-mono text-xs">{user.id}</dd>
          </div>
        </dl>
      </CardContent>
    </Card>
  )
}

/**
 * Editor for the user's default market.
 *
 * Submission requires both fields, since both are NotBlank server-side, and is disabled while the
 * form is unchanged so the button reflects whether there is anything to save.
 */
function PreferencesCard({
  user,
  onSaved,
}: {
  user: import('@/types/api').UserDto
  onSaved: (user: import('@/types/api').UserDto) => void
}) {
  const [dataCenter, setDataCenter] = React.useState(user.defaultDataCenter)
  const [world, setWorld] = React.useState(user.defaultWorld)
  const [saving, setSaving] = React.useState(false)
  const [error, setError] = React.useState<ApiError | null>(null)

  const dirty = dataCenter !== user.defaultDataCenter || world !== user.defaultWorld

  async function handleSave(event: React.FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const updated = await api.users.updateDefaults({
        defaultDataCenter: dataCenter,
        defaultWorld: world,
      })
      onSaved(updated)
      toast.success('Default market updated')
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Could not save your preferences.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card id="preferences">
      <CardHeader>
        <CardTitle className="text-base">Default market</CardTitle>
        <CardDescription>
          Used to pre-fill the pricing scope on the craft cost page and new saved crafts.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSave} className="space-y-4" noValidate>
          {error && (
            <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm">
              {error.message}
            </p>
          )}

          <WorldDataCenterPicker
            dataCenter={dataCenter}
            world={world}
            onChange={(next) => {
              setDataCenter(next.dataCenter)
              setWorld(next.world)
            }}
            dataCenterError={error?.fieldError('defaultDataCenter')}
            worldError={error?.fieldError('defaultWorld')}
            disabled={saving}
          />

          <div className="flex items-center gap-3">
            <Button type="submit" disabled={!dirty || saving || !dataCenter || !world}>
              {saving ? <Loader2 className="animate-spin" /> : <Check />}
              Save changes
            </Button>
            {dirty && !saving && <span className="text-xs text-muted-foreground">Unsaved changes</span>}
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

function AppearanceCard() {
  const { theme, toggleTheme } = useTheme()

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Appearance</CardTitle>
        <CardDescription>Remembered in this browser.</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-sm">
            {theme === 'dark' ? <Moon className="size-4" /> : <Sun className="size-4" />}
            <span className="capitalize">{theme} theme</span>
          </div>
          <Button variant="outline" size="sm" onClick={toggleTheme}>
            Switch to {theme === 'dark' ? 'light' : 'dark'}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

/** Small activity summary, derived from the saved-craft list the user already has access to. */
function ActivityCard() {
  const { data, loading } = useAsync((signal) => api.savedCrafts.list(signal), [])

  const totalRecipes = (data ?? []).reduce((sum, craft) => sum + craft.recipeCount, 0)

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Activity</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex items-baseline justify-between gap-2">
          <span className="text-sm text-muted-foreground">Lists</span>
          <span className="tabular text-xl font-semibold">
            {loading ? '—' : formatNumber(data?.length ?? 0)}
          </span>
        </div>
        <div className="flex items-baseline justify-between gap-2">
          <span className="text-sm text-muted-foreground">Recipes tracked</span>
          <span className="tabular text-xl font-semibold">{loading ? '—' : formatNumber(totalRecipes)}</span>
        </div>
        <Button variant="outline" size="sm" asChild className="w-full">
          <Link to="/lists">
            <Bookmark /> View lists
          </Link>
        </Button>
      </CardContent>
    </Card>
  )
}
