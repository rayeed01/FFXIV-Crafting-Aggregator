package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.DataCenterDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.WorldDto;

import java.util.List;

/**
 * Read-only enumeration of the synced worlds and data centers, for populating client selectors.
 *
 * <p>Distinct from {@link WorldRegistry}, which answers "is this name valid" during request
 * validation and keeps a cached snapshot for that purpose. This one lists, and reads through every
 * time rather than sharing that snapshot.
 */
public interface WorldService {

    /**
     * Every synced world, ordered by name, each with its data center and region.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.GameServerDataNotSyncedException
     *         if no worlds have been synced. Throwing rather than returning an empty list is
     *         deliberate: an empty array renders as a silently empty dropdown with nothing to act
     *         on, where the exception carries a message saying a sync is owed.
     */
    List<WorldDto> getAllWorlds();

    /**
     * Every synced data center, ordered by name.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.GameServerDataNotSyncedException
     *         if none have been synced, for the same reason as above
     */
    List<DataCenterDto> getAllDataCenters();
}
