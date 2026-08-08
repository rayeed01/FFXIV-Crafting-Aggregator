import * as React from 'react'
import { ApiError, api } from '@/lib/api'
import type { DataCenterDto, WorldDto } from '@/types/api'

interface GameServers {
  worlds: WorldDto[]
  dataCenters: DataCenterDto[]
  worldsByDataCenter: Map<string, WorldDto[]>
}

// The world list only changes when an admin runs a sync, so it is fetched once per page load
// and shared. Without this every selector on every route would refetch ~100 rows.
let cache: Promise<GameServers> | null = null

function loadGameServers(): Promise<GameServers> {
  cache ??= Promise.all([api.worlds.list(), api.worlds.dataCenters()])
    .then(([worlds, dataCenters]) => {
      const worldsByDataCenter = new Map<string, WorldDto[]>()
      for (const world of worlds) {
        const bucket = worldsByDataCenter.get(world.dataCenter)
        if (bucket) bucket.push(world)
        else worldsByDataCenter.set(world.dataCenter, [world])
      }
      return { worlds, dataCenters, worldsByDataCenter }
    })
    .catch((error: unknown) => {
      cache = null // let a later mount retry rather than caching the failure forever
      throw error
    })

  return cache
}

export function useWorlds() {
  const [data, setData] = React.useState<GameServers | null>(null)
  const [error, setError] = React.useState<ApiError | null>(null)
  const [loading, setLoading] = React.useState(true)

  React.useEffect(() => {
    let cancelled = false

    loadGameServers()
      .then((result) => {
        if (!cancelled) setData(result)
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err : new ApiError(0, 'Could not load worlds.'))
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  /**
   * Universalis world id -> display name. CraftCostNode reports the cheapest listing's world as
   * a numeric id, which is meaningless to a player without this lookup.
   */
  const worldNameById = React.useMemo(() => {
    const map = new Map<number, string>()
    for (const world of data?.worlds ?? []) map.set(world.universalisId, world.name)
    return map
  }, [data])

  return {
    worlds: data?.worlds ?? [],
    dataCenters: data?.dataCenters ?? [],
    worldsByDataCenter: data?.worldsByDataCenter ?? new Map<string, WorldDto[]>(),
    worldNameById,
    loading,
    error,
  }
}

/** Called after an admin world sync so selectors pick up newly added worlds without a reload. */
export function invalidateWorldCache(): void {
  cache = null
}
