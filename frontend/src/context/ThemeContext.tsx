import * as React from 'react'

type Theme = 'light' | 'dark'

const STORAGE_KEY = 'ffxiv-theme'

interface ThemeContextValue {
  theme: Theme
  toggleTheme: () => void
}

const ThemeContext = React.createContext<ThemeContextValue | null>(null)

function initialTheme(): Theme {
  // Mirrors the inline script in index.html, which has already applied the class by now.
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored === 'light' || stored === 'dark') return stored
  } catch {
    /* private mode */
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = React.useState<Theme>(initialTheme)

  React.useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    try {
      localStorage.setItem(STORAGE_KEY, theme)
    } catch {
      /* non-fatal */
    }
  }, [theme])

  const value = React.useMemo<ThemeContextValue>(
    () => ({ theme, toggleTheme: () => setTheme((t) => (t === 'dark' ? 'light' : 'dark')) }),
    [theme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const context = React.useContext(ThemeContext)
  if (!context) throw new Error('useTheme must be used inside <ThemeProvider>')
  return context
}
