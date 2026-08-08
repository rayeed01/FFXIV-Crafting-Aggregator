import * as React from 'react'
import { ApiError } from '@/lib/api'

interface AsyncState<T> {
  data: T | null
  error: ApiError | null
  loading: boolean
}

/**
 * Runs an async loader whenever `deps` change.
 *
 * The in-flight request is aborted on dependency change and on unmount, so a slow earlier
 * response can never overwrite a newer one - the classic out-of-order search result bug.
 *
 * The loader is held in a ref so callers need not memoise it; the `deps` array is the single
 * source of truth for when to re-run. An AbortError is swallowed rather than surfaced, because a
 * cancellation is not a failure the user should see.
 *
 * @param loader  receives an AbortSignal to pass through to fetch
 * @param deps    re-runs the loader when any of these change
 * @param enabled defer entirely, for a query that has not been entered yet
 * @returns the current data, error and loading state, plus `reload` to re-run on demand
 */
export function useAsync<T>(
  loader: (signal: AbortSignal) => Promise<T>,
  deps: React.DependencyList,
  enabled = true,
) {
  const [state, setState] = React.useState<AsyncState<T>>({ data: null, error: null, loading: enabled })
  const [reloadToken, setReloadToken] = React.useState(0)

  const loaderRef = React.useRef(loader)
  React.useEffect(() => {
    loaderRef.current = loader
  })

  React.useEffect(() => {
    if (!enabled) {
      setState({ data: null, error: null, loading: false })
      return
    }

    const controller = new AbortController()
    setState((prev) => ({ ...prev, loading: true, error: null }))

    loaderRef
      .current(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setState({ data, error: null, loading: false })
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        if (error instanceof DOMException && error.name === 'AbortError') return
        setState({
          data: null,
          loading: false,
          error: error instanceof ApiError ? error : new ApiError(0, 'Could not reach the server.'),
        })
      })

    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, enabled, reloadToken])

  const reload = React.useCallback(() => setReloadToken((n) => n + 1), [])

  return { ...state, reload }
}

/**
 * Debounces a rapidly-changing value so a search box does not fire a request per keystroke.
 *
 * @param delayMs quiet period before the value is adopted
 */
export function useDebounced<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = React.useState(value)

  React.useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}
