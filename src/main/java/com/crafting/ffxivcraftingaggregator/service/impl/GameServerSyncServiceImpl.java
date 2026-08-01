package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.client.UniversalisClient;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisDataCenter;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisWorld;
import com.crafting.ffxivcraftingaggregator.domain.dto.GameServerSyncResult;
import com.crafting.ffxivcraftingaggregator.domain.entity.DataCenter;
import com.crafting.ffxivcraftingaggregator.domain.entity.World;
import com.crafting.ffxivcraftingaggregator.exception.GameServerSyncException;
import com.crafting.ffxivcraftingaggregator.repository.DataCenterRepository;
import com.crafting.ffxivcraftingaggregator.repository.WorldRepository;
import com.crafting.ffxivcraftingaggregator.service.GameServerSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerSyncServiceImpl implements GameServerSyncService {

    private final UniversalisClient universalisClient;
    private final WorldRepository worldRepository;
    private final DataCenterRepository dataCenterRepository;

    @Override
    @Transactional
    public GameServerSyncResult sync() {
        List<UniversalisDataCenter> dataCenterPayload;
        List<UniversalisWorld> worldPayload;
        try {
            dataCenterPayload = universalisClient.getDataCenters();
            worldPayload = universalisClient.getWorlds();
        }catch (RestClientException ex){
            throw new GameServerSyncException("Universalis request failed during sync");
        }

        if(dataCenterPayload.isEmpty() || worldPayload.isEmpty()){
            throw new GameServerSyncException("Universalis returned an empty payload (dataCenters=%d, worlds=%d); aborting sync"
                    .formatted(dataCenterPayload.size(), worldPayload.size()));
        }

        Map<Integer, String> dataCenterNameByWorldId = new HashMap<>();
        for(UniversalisDataCenter dc : dataCenterPayload){
            if(dc.worlds() == null){
                continue;
            }
            for(Integer worldId : dc.worlds()){
                dataCenterNameByWorldId.put(worldId,dc.name());
            }
        }

        Map<String,DataCenter> savedDataCenters = upsertDataCenters(dataCenterPayload);

        return upsertWorlds(worldPayload,dataCenterNameByWorldId, savedDataCenters);
    }

    private Map<String, DataCenter> upsertDataCenters(List<UniversalisDataCenter> payload){
        Map<String, DataCenter> existing = dataCenterRepository.findAll().stream()
                .collect(Collectors.toMap(DataCenter::getName, Function.identity()));

        List<DataCenter> toSave = new ArrayList<>();

        for(UniversalisDataCenter dc : payload){
            DataCenter entity = existing.get(dc.name());

            if(entity == null){
                entity = DataCenter.builder()
                        .name(dc.name())
                        .region(dc.region())
                        .build();
            }
            else {
                entity.setRegion(dc.region());
            }
            toSave.add(entity);
        }
        return dataCenterRepository.saveAll(toSave).stream()
                .collect(Collectors.toMap(DataCenter::getName, Function.identity()));
    }

    private GameServerSyncResult upsertWorlds(List<UniversalisWorld> payload,
                                              Map<Integer,String> dataCenterNameByWorldId,
                                              Map<String,DataCenter> dataCenterByName){
        Map<Integer, World> existing = worldRepository.findAll().stream()
                .collect(Collectors.toMap(World::getUniversalisId, Function.identity()));

        List<World> toSave = new ArrayList<>();
        int skipped = 0;

        for(UniversalisWorld world: payload){

            String dataCenterName = dataCenterNameByWorldId.get(world.id());
            DataCenter dataCenter = (dataCenterName == null) ? null : dataCenterByName.get(dataCenterName);

            if (dataCenter == null) {
                log.warn("Skipping world {} ({}): no data center assignment in the Universalis payload",
                        world.id(), world.name());
                skipped++;
                continue;
            }

            World entity = existing.get(world.id());

            if(entity == null){
                entity = World.builder()
                        .universalisId(world.id())
                        .name(world.name())
                        .dataCenter(dataCenter)
                        .build();
            }
            else {
                entity.setName(world.name());
                entity.setDataCenter(dataCenter);
            }
            toSave.add(entity);
        }
        List<World> saved = worldRepository.saveAll(toSave);

        log.info("Game server sync complete: {} data centers, {} worlds, {} skipped",
                dataCenterByName.size(), saved.size(), skipped);

        return new GameServerSyncResult(dataCenterByName.size(), saved.size(), skipped);
    }
}
