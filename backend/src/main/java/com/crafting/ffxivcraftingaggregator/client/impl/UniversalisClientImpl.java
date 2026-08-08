package com.crafting.ffxivcraftingaggregator.client.impl;

import com.crafting.ffxivcraftingaggregator.client.UniversalisClient;
import com.crafting.ffxivcraftingaggregator.client.dto.ItemPrice;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisAggregatedResponse;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisAggregatedResponse.AggregatedResult;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisAggregatedResponse.PriceEntry;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisAggregatedResponse.QualityData;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisDataCenter;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisWorld;
import com.crafting.ffxivcraftingaggregator.exception.UniversalisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UniversalisClientImpl implements UniversalisClient {
    private static final int MAX_BATCH_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(UniversalisClientImpl.class);
    private final RestClient restClient;

    public UniversalisClientImpl(RestClient.Builder builder,
                                 @Value("${universalis.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public UniversalisPrices getPrices(List<Integer> itemXivapiIds, String scope) {
        if (itemXivapiIds == null || itemXivapiIds.isEmpty()) {
            return UniversalisPrices.empty();
        }

        List<Integer> distinctIds = itemXivapiIds.stream().distinct().toList();

        if (distinctIds.size() <= MAX_BATCH_SIZE) {
            return fetchChunk(distinctIds, scope);
        }

        UniversalisPrices merged = UniversalisPrices.empty();
        for (int start = 0; start < distinctIds.size(); start += MAX_BATCH_SIZE) {
            int end = Math.min(start + MAX_BATCH_SIZE, distinctIds.size());
            merged = merged.merge(fetchChunk(distinctIds.subList(start, end), scope));
        }
        return merged;
    }

    @Override
    public List<UniversalisWorld> getWorlds() {
        List<UniversalisWorld> worlds = restClient.get()
                .uri("/api/v2/worlds")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return worlds == null ? List.of() : worlds;
    }

    @Override
    public List<UniversalisDataCenter> getDataCenters() {
        List<UniversalisDataCenter> dataCenters = restClient.get()
                .uri("/api/v2/data-centers")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return dataCenters == null ? List.of() : dataCenters;
    }

    private UniversalisPrices fetchChunk(List<Integer> chunk, String scope) {

        String ids = chunk.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        UniversalisAggregatedResponse response;
        try {
            response = restClient.get()
                    .uri("/api/v2/aggregated/{scope}/{ids}", scope, ids)
                    .retrieve()
                    .body(UniversalisAggregatedResponse.class);
        } catch (HttpClientErrorException ex) {
            log.warn("Universalis rejected chunk of {} ids for scope {}: {}",
                    chunk.size(), scope, ex.getStatusCode());
            return new UniversalisPrices(Map.of(), new HashSet<>(chunk));
        } catch (RestClientException ex) {
            throw new UniversalisException("Universalis request failed for scope " + scope, ex);
        }

        if (response == null) {
            return UniversalisPrices.empty();
        }

        Map<Integer, ItemPrice> prices = new HashMap<>();
        if (response.results() != null) {
            for (AggregatedResult result : response.results()) {
                prices.put(result.itemId(), toItemPrice(result));
            }
        }

        Set<Integer> unresolved = (response.failedItems() == null)
                ? new HashSet<>()
                : new HashSet<>(response.failedItems());

        Set<Integer> accountedFor = new HashSet<>(prices.keySet());
        accountedFor.addAll(unresolved);

        Set<Integer> missing = new HashSet<>(chunk);
        missing.removeAll(accountedFor);
        unresolved.addAll(missing);

        return new UniversalisPrices(prices, unresolved);
    }

    private ItemPrice toItemPrice(AggregatedResult result) {

        PriceEntry nq = pickScope(result.nq());
        PriceEntry hq = pickScope(result.hq());

        Long nqPrice = (nq == null) ? null : nq.price();
        Long hqPrice = (hq == null) ? null : hq.price();

        if (nqPrice == null && hqPrice == null) {

            return ItemPrice.unlisted(result.itemId());
        }

        Long minPrice;
        Integer cheapestWorldId;

        if (nqPrice == null) {
            minPrice = hqPrice;
            cheapestWorldId = hq.worldId();
        } else if (hqPrice == null || nqPrice <= hqPrice) {
            minPrice = nqPrice;
            cheapestWorldId = nq.worldId();
        } else {
            minPrice = hqPrice;
            cheapestWorldId = hq.worldId();
        }

        return new ItemPrice(
                result.itemId(),
                minPrice,
                nqPrice,
                hqPrice,
                cheapestWorldId,
                (nq == null) ? null : nq.worldId(),
                (hq == null) ? null : hq.worldId());
    }

    private PriceEntry pickScope(QualityData quality) {
        if (quality == null || quality.minListing() == null) {
            return null;
        }
        if (quality.minListing().world() != null) {
            return quality.minListing().world();
        }
        return quality.minListing().dc();
    }
}