import * as React from 'react'

type Theme = 'light' | 'dark'

const STORAGE_KEY = 'ffxiv-theme'

interface ThemeContextValue {
  theme: Theme
  toggleTheme: () => void
}

const ThemeContext = React.createContext<ThemeContextValue | null>(null)

/**
 * Resolves the starting theme from storage, falling back to the OS preference.
 *
 * Mirrors the inline script in index.html, which has already applied the class to the document by
 * the time React mounts. Storage access is guarded because private browsing can throw on read.
 */
function initialTheme(): Theme {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored === 'light' || stored === 'dark') return stored
  } catch {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/**
 * Supplies the current theme and a toggle, keeping the `dark` class on the document in step.
 *
 * Persisting to localStorage is best-effort; a failure only means the choice does not survive a
 * reload, which is not worth failing the render over.
 */
export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = React.useState<Theme>(initialTheme)

  React.useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    try {
      localStorage.setItem(STORAGE_KEY, theme)
    } catch {
      /* best-effort */
    }
  }, [theme])

  const value = React.useMemo<ThemeContextValue>(
    () => ({ theme, toggleTheme: () => setTheme((t) => (t === 'dark' ? 'light' : 'dark')) }),
    [theme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

/**
 * Reads the theme context.
 *
 * @throws Error if used outside a {@link ThemeProvider}
 */
export function useTheme(): ThemeContextValue {
  const context = React.useContext(ThemeContext)
  if (!context) throw new Error('useTheme must be used inside <ThemeProvider>')
  return context
}
