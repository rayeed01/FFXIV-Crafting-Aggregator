package com.crafting.ffxivcraftingaggregator.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UniversalisPriceResponse(@JsonProperty("itemID") int itemId,
                                       @JsonProperty("minPrice") Long minPrice,
                                       @JsonProperty("minPriceNQ") Long minPriceNq,
                                       @JsonProperty("minPriceHQ") Long minPriceHq,
                                       @JsonProperty("hasData") boolean hasData,
                                       @JsonProperty("lastUploadTime") Long lastUploadTime) {
}
