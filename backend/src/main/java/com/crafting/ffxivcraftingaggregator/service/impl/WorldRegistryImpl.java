package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.entity.DataCenter;
import com.crafting.ffxivcraftingaggregator.domain.entity.World;
import com.crafting.ffxivcraftingaggregator.exception.GameServerDataNotSyncedException;
import com.crafting.ffxivcraftingaggregator.exception.UnknownDataCenterException;
import com.crafting.ffxivcraftingaggregator.exception.UnknownWorldException;
import com.crafting.ffxivcraftingaggregator.exception.WorldDataCenterMismatchException;
import com.crafting.ffxivcraftingaggregator.repository.DataCenterRepository;
import com.crafting.ffxivcraftingaggregator.repository.WorldRepository;
import com.crafting.ffxivcraftingaggregator.service.WorldRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * World and data center validation against a cached snapshot of the world tables.
 *
 * <p>The snapshot is immutable and swapped wholesale rather than mutated, so a lookup in progress
 * during a refresh sees a consistent view rather than a half-updated map. It is held in a volatile
 * field and built lazily on first use, with the double-checked block ensuring one build rather
 * than one per concurrent caller.
 *
 * <p>Names are normalised to lower case for lookup while the original casing is kept as the
 * canonical form, which is what lets a caller type "aether" and still have "Aether" sent upstream.
 */
@Component
@RequiredArgsConstructor
public class WorldRegistryImpl implements WorldRegistry {

    private static final Logger logger = LoggerFactory.getLogger(WorldRegistryImpl.class);

    private final WorldRepository worldRepository;
    private final DataCenterRepository dataCenterRepository;

    private volatile Snapshot snapshot;

    @Override
    public String canonicalWorldName(String input) {
        String canonical = currentSnapshot().worlds().get(normalise(input));

        if(canonical == null){
            throw new UnknownWorldException("Unknown world: %s".formatted(input));
        }
        return canonical;
    }

    @Override
    public String canonicalDataCenterName(String input) {
        String canonical = currentSnapshot().dataCenters().get(normalise(input));

        if(canonical == null){
            throw new UnknownDataCenterException("Unknown datacenter: %s".formatted(input));
        }
        return canonical;
    }

    @Override
    public void validateWorldBelongsToDataCenter(String worldName, String dataCenterName) {
        String canonicalWorld = canonicalWorldName(worldName);
        String canonicalDataCenter = canonicalDataCenterName(dataCenterName);

        String actualDataCenter = currentSnapshot().dataCenterByWorld.get(normalise(canonicalWorld));

        if(!canonicalDataCenter .equals(actualDataCenter)){
            throw new WorldDataCenterMismatchException("%s is on %s not %s".formatted(canonicalWorld, actualDataCenter, canonicalDataCenter));
        }

    }

    @Override
    public String dataCenterForWorld(String worldName) {
        String canonicalWorld = canonicalWorldName(worldName);
        return currentSnapshot().dataCenterByWorld.get(normalise(canonicalWorld));
    }

    @Override
    public void refresh() {
        this.snapshot = load();
        logger.info("World registry refreshed: {} worlds, {} data centers", snapshot.worlds.size(), snapshot.dataCenters.size());
    }

    private String normalise(String name){
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
    
    private Snapshot currentSnapshot(){
        Snapshot current = snapshot;
        if(current == null){
            synchronized(this){
                if(snapshot == null){
                    snapshot = load();
                }
                current = snapshot;
            }
        }
        return current;
    }

    private Snapshot load(){
        List<World> worlds = worldRepository.findAll();
        List<DataCenter> dataCenters = dataCenterRepository.findAll();

        if(worlds.isEmpty() || dataCenters.isEmpty()){
            throw new GameServerDataNotSyncedException("World data not synced yet, run POST /api/v1/admin/sync/worlds");
        }

        Map<String, String> worldNames = new HashMap<>();
        Map<String, String> dataCenterByWorld = new HashMap<>();
        for(World world: worlds){
            worldNames.put(normalise(world.getName()), world.getName());
            dataCenterByWorld.put(normalise(world.getName()), world.getDataCenter().getName());
        }

        Map<String, String> dataCenterNames = new HashMap<>();
        for(DataCenter datacenter: dataCenters){
            dataCenterNames.put(normalise(datacenter.getName()), datacenter.getName());
        }

        return new Snapshot(Map.copyOf(worldNames),
                Map.copyOf(dataCenterNames),
                Map.copyOf(dataCenterByWorld));
    }
    

    private record Snapshot(Map<String, String> worlds,
                            Map<String, String> dataCenters,
                            Map<String, String> dataCenterByWorld){}
}
