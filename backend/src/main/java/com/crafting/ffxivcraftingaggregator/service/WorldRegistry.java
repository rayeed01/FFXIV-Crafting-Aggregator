package com.crafting.ffxivcraftingaggregator.service;

/**
 * Validates and canonicalises world and data center names.
 *
 * <p>Names arrive from clients in whatever case and spacing the user typed, but Universalis is
 * exact about them, so every name is normalised here before it reaches an upstream call.
 *
 * <p>Backed by a cached snapshot of the world tables rather than a query per check, because these
 * names are validated on nearly every pricing request and the underlying data only changes when
 * an admin runs a sync. {@link #refresh()} is how that sync publishes its results.
 *
 * <p>Distinct from {@link WorldService}, which enumerates worlds for display. This one answers
 * "is this name valid, and what is its canonical form".
 */
public interface WorldRegistry {

    /**
     * @return the canonically-cased world name
     * @throws com.crafting.ffxivcraftingaggregator.exception.UnknownWorldException if unrecognised
     * @throws com.crafting.ffxivcraftingaggregator.exception.GameServerDataNotSyncedException
     *         if the world tables are empty
     */
    String canonicalWorldName(String input);

    /**
     * @return the canonically-cased data center name
     * @throws com.crafting.ffxivcraftingaggregator.exception.UnknownDataCenterException if unrecognised
     * @throws com.crafting.ffxivcraftingaggregator.exception.GameServerDataNotSyncedException
     *         if the world tables are empty
     */
    String canonicalDataCenterName(String input);

    /**
     * Asserts that a world actually sits on the given data center.
     *
     * <p>Each name can be valid on its own while the pair is nonsense, and an inconsistent pair
     * would silently price against the wrong market.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.WorldDataCenterMismatchException
     *         if the world belongs to a different data center
     */
    void validateWorldBelongsToDataCenter(String worldName, String dataCenterName);

    /**
     * @throws com.crafting.ffxivcraftingaggregator.exception.UnknownWorldException if the world is unrecognised
     */
    String dataCenterForWorld(String worldName);

    /** Rebuilds the cached snapshot. Called after a world sync so new worlds validate immediately. */
    void refresh();

}
