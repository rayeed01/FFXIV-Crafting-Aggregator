import * as React from 'react'
import { ApiError } from '@/lib/api'

interface AsyncState<T> {
  data: T | null
  error: ApiError | null
  loading: boolean
}

/**
 * Runs an async loader whenever `deps` change, with the in-flight request aborted on change or
 * unmount so a slow earlier response cannot overwrite a newer one.
 *
 * Pass `enabled: false` to defer - used where a query string has not been entered yet.
 */
export function useAsync<T>(
  loader: (signal: AbortSignal) => Promise<T>,
  deps: React.DependencyList,
  enabled = true,
) {
  const [state, setState] = React.useState<AsyncState<T>>({ data: null, error: null, loading: enabled })
  const [reloadToken, setReloadToken] = React.useState(0)

  // Held in a ref so `loader` need not be memoised by every caller; the deps array is the
  // single source of truth for when to re-run.
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
        // An aborted fetch surfaces as AbortError; that is a cancellation, not a failure.
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

/** Debounce a rapidly-changing value, so search boxes do not fire a request per keystroke. */
export function useDebounced<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = React.useState(value)

  React.useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}
