import * as React from 'react'
import { Package } from 'lucide-react'
import { iconUrl } from '@/lib/icons'
import { cn } from '@/lib/utils'

/**
 * Item icon with a placeholder fallback.
 *
 * Falls back on load error as well as on a missing URL: XIVAPI has no asset for every id, and a
 * broken-image glyph in a dense list looks like the app is failing rather than the icon.
 *
 * The failure flag resets whenever `src` changes, since a new image deserves a fresh attempt -
 * without that the placeholder would stick for the rest of the component's life.
 */
export function ItemIcon({
  src,
  alt,
  className,
}: {
  src: string | null | undefined
  alt: string
  className?: string
}) {
  const [failed, setFailed] = React.useState(false)
  const url = iconUrl(src)

  React.useEffect(() => setFailed(false), [src])

  if (!url || failed) {
    return (
      <span
        className={cn('grid size-9 shrink-0 place-items-center rounded-md bg-secondary', className)}
        title={alt}
      >
        <Package className="size-4 text-muted-foreground" />
      </span>
    )
  }

  return (
    <img
      src={url}
      alt=""
      aria-hidden
      title={alt}
      loading="lazy"
      onError={() => setFailed(true)}
      className={cn('size-9 shrink-0 rounded-md bg-secondary object-contain', className)}
    />
  )
}
