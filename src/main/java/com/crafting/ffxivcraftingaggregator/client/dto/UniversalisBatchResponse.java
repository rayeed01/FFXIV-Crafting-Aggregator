package com.crafting.ffxivcraftingaggregator.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record UniversalisBatchResponse(@JsonProperty("itemIDs")List<Integer> itemIds,
                                       @JsonProperty("items") Map<String, UniversalisPriceResponse> items) {
}
