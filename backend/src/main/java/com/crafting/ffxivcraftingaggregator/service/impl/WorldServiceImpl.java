package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.DataCenterDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.WorldDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.DataCenter;
import com.crafting.ffxivcraftingaggregator.domain.entity.World;
import com.crafting.ffxivcraftingaggregator.exception.GameServerDataNotSyncedException;
import com.crafting.ffxivcraftingaggregator.repository.DataCenterRepository;
import com.crafting.ffxivcraftingaggregator.repository.WorldRepository;
import com.crafting.ffxivcraftingaggregator.service.WorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only view of the synced world / data center tables, for populating client selectors.
 *
 * <p>Distinct from {@link com.crafting.ffxivcraftingaggregator.service.WorldRegistry}, which answers
 * "is this name valid" during request validation and keeps a cached snapshot for that purpose. This
 * enumerates, so it reads through every time rather than sharing that snapshot.
 */
@Service
@RequiredArgsConstructor
public class WorldServiceImpl implements WorldService {

    private final WorldRepository worldRepository;
    private final DataCenterRepository dataCenterRepository;

    /**
     * Empty tables throw rather than returning [] so the caller gets the 503 explaining that a sync
     * is owed. An empty array would render as a silently empty dropdown with nothing to act on.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WorldDto> getAllWorlds() {
        List<World> worlds = worldRepository.findAllWithDataCenter();

        if (worlds.isEmpty()) {
            throw new GameServerDataNotSyncedException("World data not synced yet, run POST /api/v1/admin/sync/worlds");
        }

        return worlds.stream()
                .map(world -> WorldDto.builder()
                        .name(world.getName())
                        .universalisId(world.getUniversalisId())
                        .dataCenter(world.getDataCenter().getName())
                        .region(world.getDataCenter().getRegion())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataCenterDto> getAllDataCenters() {
        List<DataCenter> dataCenters = dataCenterRepository.findAll(Sort.by("name"));

        if (dataCenters.isEmpty()) {
            throw new GameServerDataNotSyncedException("World data not synced yet, run POST /api/v1/admin/sync/worlds");
        }

        return dataCenters.stream()
                .map(dataCenter -> DataCenterDto.builder()
                        .name(dataCenter.getName())
                        .region(dataCenter.getRegion())
                        .build())
                .toList();
    }
}
