/**
 * The sync stores XIVAPI's CraftType name ("Smithing"), not the job that performs it
 * ("Blacksmith"). Players think in jobs, so everything user-facing goes through this map.
 *
 * All eight Disciples of the Hand are covered, and the set is closed - the game has not added a
 * crafting class since 2.0 - so an unmatched value means bad data rather than a missing entry.
 *
 * iconId is the ClassJob row id; icons live at ui/icon/062000/0620{id}.tex.
 */
export interface JobInfo {
  job: string
  abbr: string
  iconId: number
}

const BY_CRAFT_TYPE: Record<string, JobInfo> = {
  woodworking: { job: 'Carpenter', abbr: 'CRP', iconId: 8 },
  smithing: { job: 'Blacksmith', abbr: 'BSM', iconId: 9 },
  armorcraft: { job: 'Armorer', abbr: 'ARM', iconId: 10 },
  goldsmithing: { job: 'Goldsmith', abbr: 'GSM', iconId: 11 },
  leatherworking: { job: 'Leatherworker', abbr: 'LTW', iconId: 12 },
  clothcraft: { job: 'Weaver', abbr: 'WVR', iconId: 13 },
  alchemy: { job: 'Alchemist', abbr: 'ALC', iconId: 14 },
  cooking: { job: 'Culinarian', abbr: 'CUL', iconId: 15 },
}

// Also accept the job names themselves, so a future sync that stores "Blacksmith" directly
// keeps working without a data migration.
const BY_JOB_NAME: Record<string, JobInfo> = Object.fromEntries(
  Object.values(BY_CRAFT_TYPE).map((info) => [info.job.toLowerCase(), info]),
)

export function jobInfo(craftTypeOrJob: string | null | undefined): JobInfo | null {
  if (!craftTypeOrJob) return null
  const key = craftTypeOrJob.trim().toLowerCase()
  return BY_CRAFT_TYPE[key] ?? BY_JOB_NAME[key] ?? null
}

/** Display name for a craft type, falling back to the raw value rather than hiding it. */
export function jobName(craftTypeOrJob: string | null | undefined): string | null {
  if (!craftTypeOrJob) return null
  return jobInfo(craftTypeOrJob)?.job ?? craftTypeOrJob
}

export function jobIconPath(info: JobInfo): string {
  return `ui/icon/062000/0620${String(info.iconId).padStart(2, '0')}.tex`
}
