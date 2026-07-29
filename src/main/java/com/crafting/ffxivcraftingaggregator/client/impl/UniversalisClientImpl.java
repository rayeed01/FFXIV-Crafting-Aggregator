package com.crafting.ffxivcraftingaggregator.client.impl;

import com.crafting.ffxivcraftingaggregator.client.UniversalisClient;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisBatchResponse;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPriceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UniversalisClientImpl implements UniversalisClient {

    private final RestClient restClient;

    public UniversalisClientImpl(RestClient.Builder builder,
                                 @Value("${universalis.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Map<Integer, UniversalisPriceResponse> getPrices(List<Integer> itemXivapiIds, String worldOrDc) {
        if(itemXivapiIds.isEmpty()){
            return Map.of();
        }

        String ids = itemXivapiIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        if(itemXivapiIds.size() == 1){
            UniversalisPriceResponse single = restClient.get()
                    .uri("/api/v2/{worldOrDc}/{ids}", worldOrDc, ids)
                    .retrieve()
                    .body(UniversalisPriceResponse.class);

            if(single == null){
                return Map.of();
            }
            return Map.of(single.itemId(),single);
        }


        UniversalisBatchResponse response = restClient.get()
                .uri("/api/v2/{worldOrDc}/{ids}", worldOrDc, ids)
                .retrieve()
                .body(UniversalisBatchResponse.class);

        if(response == null || response.items() == null){
            return Map.of();
        }

        return response.items().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Integer.parseInt(e.getKey()),
                        Map.Entry::getValue
                ));
    }
}
