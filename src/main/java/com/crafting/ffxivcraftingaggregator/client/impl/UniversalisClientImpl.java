package com.crafting.ffxivcraftingaggregator.client.impl;

import com.crafting.ffxivcraftingaggregator.client.UniversalisClient;
import com.crafting.ffxivcraftingaggregator.client.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import org.springframework.web.client.RestClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class UniversalisClientImpl implements UniversalisClient {

    private static final int MAX_BATCH_SIZE = 100;

    private final RestClient restClient;

    public UniversalisClientImpl(RestClient.Builder builder,
                                 @Value("${universalis.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public UniversalisPrices getPrices(List<Integer> itemXivapiIds, String worldOrDc) {
        if(itemXivapiIds == null || itemXivapiIds.isEmpty()){
            return UniversalisPrices.empty();
        }

        List<Integer> distinctIds = itemXivapiIds.stream()
                .distinct()
                .toList();

        if(distinctIds.size() <= MAX_BATCH_SIZE){
            return fetchChuck(distinctIds,worldOrDc);
        }

        UniversalisPrices merged = UniversalisPrices.empty();
        for(int start = 0; start < distinctIds.size(); start += MAX_BATCH_SIZE){
            int end = Math.min(start + MAX_BATCH_SIZE, distinctIds.size());
            merged = merged.merge(fetchChuck(distinctIds.subList(start,end), worldOrDc));
        }
        return  merged;
    }

    @Override
    public List<UniversalisWorld> getWorlds() {
        List<UniversalisWorld> worlds = restClient.get()
                .uri("/api/v2/world")
                .retrieve()
                .body(new ParameterizedTypeReference<List<UniversalisWorld>>() {});

        return worlds == null ? List.of() : worlds;
    }

    @Override
    public List<UniversalisDataCenter> getDataCenters() {
        List<UniversalisDataCenter> dataCenters = restClient.get()
                .uri("/api/v2/data-centers")
                .retrieve()
                .body(new ParameterizedTypeReference<List<UniversalisDataCenter>>() {});

        return dataCenters == null ? List.of() : dataCenters;
    }

    private UniversalisPrices fetchChuck(List<Integer> chunk, String worldOrDc){
        return chunk.size() == 1
                ?fetchSingle(chunk.getFirst(), worldOrDc)
                :fetchBatch(chunk, worldOrDc);
    }

    private UniversalisPrices fetchSingle(int itemXivapiId, String worldOrDc){
        UniversalisPriceResponse single;
        try{
            single = restClient.get()
                    .uri("/api/v2/{worldOrDc}/{ids}", worldOrDc, String.valueOf(itemXivapiId))
                    .retrieve()
                    .body(UniversalisPriceResponse.class);

        }catch (RestClientResponseException ex){
            if(ex.getStatusCode().is4xxClientError()){
                return new UniversalisPrices(Map.of(),Set.of(itemXivapiId));
            }
            throw ex;
        }

        if(single == null){
            return new UniversalisPrices(Map.of(), Set.of(itemXivapiId));
        }
        return new UniversalisPrices(Map.of(single.itemId(), single), Set.of());
    }

    private UniversalisPrices fetchBatch(List<Integer> chunk, String worldOrDc){
        String ids = chunk.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        UniversalisBatchResponse response = restClient.get()
                .uri("/api/v2/{worldOrDc}/{ids}", worldOrDc, ids)
                .retrieve()
                .body(UniversalisBatchResponse.class);

        if(response == null){
            return UniversalisPrices.empty();
        }

        Map<Integer, UniversalisPriceResponse> prices = (response.items() == null)
                ? Map.of()
                : response.items().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Integer.parseInt(e.getKey()),
                        Map.Entry::getValue));

        Set<Integer> unresolved = (response.unresolvedItems() == null)
                ? new HashSet<>()
                : new HashSet<>(response.unresolvedItems());

        Set<Integer> accountedFor = new HashSet<>(prices.keySet());
        accountedFor.addAll(unresolved);

        Set<Integer> missing = new HashSet<>(chunk);
        missing.removeAll(accountedFor);
        unresolved.addAll(missing);

        return new UniversalisPrices(prices, unresolved);
    }

}
