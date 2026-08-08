const gilFormatter = new Intl.NumberFormat('en-US')

/**
 * Gil amounts. Null is not 0 - the backend uses null for "no market listing / unobtainable",
 * and showing that as "0 gil" would read as free.
 */
export function formatGil(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return gilFormatter.format(value)
}

export function formatNumber(value: number): string {
  return gilFormatter.format(value)
}

/**
 * Percentage difference of craft vs buy. Returns null when either side is unknown, or when
 * buy is 0 - dividing by it yields Infinity and renders as a nonsense saving.
 */
export function percentDelta(craft: number | null, buy: number | null): number | null {
  if (craft === null || buy === null || buy === 0) return null
  return ((craft - buy) / buy) * 100
}

/**
 * Jackson emits LocalDateTime without an offset ("2026-08-07T14:22:07.123"). new Date() reads
 * that as local time, which is the intent here - these are display-only timestamps.
 */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
}

export function formatRelative(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'

  const seconds = Math.round((date.getTime() - Date.now()) / 1000)
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' })
  const divisions: [number, Intl.RelativeTimeFormatUnit][] = [
    [60, 'second'],
    [60, 'minute'],
    [24, 'hour'],
    [7, 'day'],
    [4.34524, 'week'],
    [12, 'month'],
    [Number.POSITIVE_INFINITY, 'year'],
  ]

  let duration = seconds
  for (const [amount, unit] of divisions) {
    if (Math.abs(duration) < amount) return rtf.format(Math.round(duration), unit)
    duration /= amount
  }
  return rtf.format(Math.round(duration), 'year')
}
