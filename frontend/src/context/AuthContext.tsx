import * as React from 'react'
import {
  ApiError,
  api,
  readStoredToken,
  setAuthToken,
  setUnauthorizedHandler,
  writeStoredToken,
} from '@/lib/api'
import type { LoginRequest, RegisterRequest, UserDto } from '@/types/api'

interface AuthContextValue {
  user: UserDto | null
  /** True until the stored token has been checked, so routes do not redirect prematurely. */
  initialising: boolean
  isAuthenticated: boolean
  isAdmin: boolean
  login: (payload: LoginRequest) => Promise<void>
  register: (payload: RegisterRequest) => Promise<void>
  logout: () => void
  /** Replace the cached user after a profile update, avoiding a refetch. */
  setUser: (user: UserDto) => void
}

const AuthContext = React.createContext<AuthContextValue | null>(null)

/**
 * Owns the JWT session.
 *
 * A stored token is restored on boot but only trusted once /users/me confirms it: an expired one
 * left in localStorage would otherwise render a logged-in shell with no data behind it.
 *
 * A 401 from any request anywhere clears the session exactly once, via a handler registered with
 * the API client, which covers tokens that expire mid-session.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUserState] = React.useState<UserDto | null>(null)
  const [initialising, setInitialising] = React.useState(true)

  const logout = React.useCallback(() => {
    setAuthToken(null)
    writeStoredToken(null)
    setUserState(null)
  }, [])

  React.useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  React.useEffect(() => {
    const stored = readStoredToken()
    if (!stored) {
      setInitialising(false)
      return
    }

    setAuthToken(stored)
    let cancelled = false

    api.users
      .me()
      .then((me) => {
        if (!cancelled) setUserState(me)
      })
      .catch(() => {
        if (!cancelled) {
          setAuthToken(null)
          writeStoredToken(null)
          setUserState(null)
        }
      })
      .finally(() => {
        if (!cancelled) setInitialising(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  /**
   * Adopts a freshly issued token and loads the user behind it.
   *
   * A token that cannot be resolved to a user is unusable, so it is discarded rather than left as
   * a half-open session that looks signed in but cannot load anything.
   */
  const establishSession = React.useCallback(async (token: string) => {
    setAuthToken(token)
    writeStoredToken(token)
    try {
      setUserState(await api.users.me())
    } catch (error) {
      setAuthToken(null)
      writeStoredToken(null)
      throw error instanceof ApiError
        ? error
        : new ApiError(0, 'Signed in, but the profile could not be loaded.')
    }
  }, [])

  const login = React.useCallback(
    async (payload: LoginRequest) => {
      const { token } = await api.auth.login(payload)
      await establishSession(token)
    },
    [establishSession],
  )

  const register = React.useCallback(
    async (payload: RegisterRequest) => {
      const { token } = await api.auth.register(payload)
      await establishSession(token)
    },
    [establishSession],
  )

  const value = React.useMemo<AuthContextValue>(
    () => ({
      user,
      initialising,
      isAuthenticated: user !== null,
      isAdmin: user?.role === 'ADMIN',
      login,
      register,
      logout,
      setUser: setUserState,
    }),
    [user, initialising, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = React.useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
