const XIVAPI_BASE = 'https://v2.xivapi.com'

/**
 * Turn a stored icon value into something a browser can actually render.
 *
 * The sync stores XIVAPI's raw game path - "ui/icon/025000/025301.tex". That is a texture in the
 * game's own format, not an image: pointing an <img> at it renders nothing, which is why every
 * icon appeared broken. XIVAPI's asset endpoint converts it on demand.
 *
 * Already-usable http(s) URLs are passed through, so re-syncing with a different format later
 * does not require touching this.
 */
export function iconUrl(stored: string | null | undefined): string | null {
  if (!stored) return null

  const path = stored.trim()
  if (!path) return null
  if (path.startsWith('http://') || path.startsWith('https://')) return path

  return `${XIVAPI_BASE}/api/asset?path=${encodeURIComponent(path)}&format=png`
}
