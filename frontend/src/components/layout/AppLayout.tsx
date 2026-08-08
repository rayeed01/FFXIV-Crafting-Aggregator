import { NavLink, Outlet, Link } from 'react-router-dom'
import {
  Bookmark,
  Calculator,
  LogOut,
  Moon,
  Search,
  ShieldCheck,
  Sun,
  User as UserIcon,
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useTheme } from '@/context/ThemeContext'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'

const NAV = [
  { to: '/search', label: 'Browse', icon: Search, requiresAuth: false },
  { to: '/craft-cost', label: 'Craft Cost', icon: Calculator, requiresAuth: false },
  { to: '/lists', label: 'Lists', icon: Bookmark, requiresAuth: true },
]

function ThemeToggle() {
  const { theme, toggleTheme } = useTheme()
  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={toggleTheme}
      aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
      title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
    >
      {theme === 'dark' ? <Sun /> : <Moon />}
    </Button>
  )
}

/**
 * App shell: header, primary navigation, user menu and footer, around the routed page.
 *
 * The admin entry is hidden for non-admins, but that is presentation only - the backend still
 * enforces hasRole("ADMIN") on /api/v1/admin regardless of what the menu shows.
 *
 * There is no separate Preferences entry because preferences live on the profile page, so a
 * second link would point at the same screen.
 */
export function AppLayout() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth()

  const visibleNav = NAV.filter((item) => !item.requiresAuth || isAuthenticated)

  return (
    <div className="flex min-h-dvh flex-col bg-background">
      <header className="sticky top-0 z-40 border-b border-border bg-background/85 backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center gap-4 px-4">
          <Link to="/" className="shrink-0 font-semibold tracking-tight">
            Craftwise
          </Link>

          <nav className="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto">
            {visibleNav.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  cn(
                    'inline-flex shrink-0 items-center gap-1.5 rounded-md px-2.5 py-1.5 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-accent text-accent-foreground'
                      : 'text-muted-foreground hover:bg-secondary hover:text-foreground',
                  )
                }
              >
                <Icon className="size-4" />
                <span className="hidden sm:inline">{label}</span>
              </NavLink>
            ))}
          </nav>

          <div className="flex shrink-0 items-center gap-1">
            <ThemeToggle />

            {isAuthenticated && user ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="sm" className="gap-2">
                    <span className="grid size-6 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                      {user.username.charAt(0).toUpperCase()}
                    </span>
                    <span className="hidden max-w-28 truncate sm:inline">{user.username}</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel className="font-normal">
                    <span className="block truncate font-medium">{user.username}</span>
                    <span className="block truncate text-xs text-muted-foreground">
                      {user.defaultWorld} · {user.defaultDataCenter}
                    </span>
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem asChild>
                    <Link to="/profile">
                      <UserIcon /> Profile
                    </Link>
                  </DropdownMenuItem>
                  {isAdmin && (
                    <DropdownMenuItem asChild>
                      <Link to="/admin">
                        <ShieldCheck /> Admin
                      </Link>
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onSelect={logout} className="text-destructive focus:text-destructive">
                    <LogOut /> Sign out
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <div className="flex items-center gap-1">
                <Button variant="ghost" size="sm" asChild>
                  <Link to="/login">Sign in</Link>
                </Button>
                <Button size="sm" asChild>
                  <Link to="/register">Sign up</Link>
                </Button>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>

      <footer className="border-t border-border py-5">
        <div className="mx-auto w-full max-w-6xl px-4 text-xs text-muted-foreground">
          Market data from Universalis · Game data from XIVAPI
        </div>
      </footer>
    </div>
  )
}
