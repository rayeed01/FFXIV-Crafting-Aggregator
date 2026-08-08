import * as React from 'react'
import { iconUrl } from '@/lib/icons'
import { jobIconPath, jobInfo } from '@/lib/jobs'
import { cn } from '@/lib/utils'

/**
 * Crafting job icon, e.g. the hammer-and-anvil for Blacksmith.
 *
 * Renders nothing when the craft type is missing or unrecognised - an item with no recipe simply
 * has no job, and a blank slot reads better than a placeholder implying data failed to load.
 *
 * If the asset itself fails to load the job abbreviation is shown instead, which carries the same
 * information in less space.
 *
 * @param craftType XIVAPI CraftType name such as "Smithing"; translated to a job by {@link jobInfo}
 * @param showName  append the full job name beside the icon
 */
export function JobIcon({
  craftType,
  className,
  showName = false,
}: {
  craftType: string | null | undefined
  className?: string
  showName?: boolean
}) {
  const [failed, setFailed] = React.useState(false)
  const info = jobInfo(craftType)

  if (!info) return null

  const url = iconUrl(jobIconPath(info))

  return (
    <span className="inline-flex items-center gap-1.5" title={info.job}>
      {url && !failed ? (
        <img
          src={url}
          alt=""
          aria-hidden
          loading="lazy"
          onError={() => setFailed(true)}
          className={cn('size-4 shrink-0 object-contain', className)}
        />
      ) : (
        <span className="font-mono text-[10px] font-semibold text-muted-foreground">{info.abbr}</span>
      )}
      {showName && <span>{info.job}</span>}
      <span className="sr-only">{info.job}</span>
    </span>
  )
}
