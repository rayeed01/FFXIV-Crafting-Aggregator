package com.crafting.ffxivcraftingaggregator.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw response shape of the Universalis aggregated market endpoint.
 *
 * <p>Mirrors their JSON exactly, including the nesting of quality, then scope, then figure, so all
 * the reshaping happens in one place while the rest of the application works in its own terms.
 *
 * <p>Both NQ and HQ are parsed, and each carries the world its cheapest listing sits on. Those can
 * differ, which is why the two qualities are tracked separately rather than reduced early.
 */
public record UniversalisAggregatedResponse(@JsonProperty("results") List<AggregatedResult> results,
                                            @JsonProperty("failedItems") List<Integer> failedItems) {

    public record AggregatedResult(@JsonProperty("itemId") int itemId,
                                   @JsonProperty("nq") QualityData nq,
                                   @JsonProperty("hq") QualityData hq,
                                   @JsonProperty("worldUploadTimes") List<WorldUploadTime> worldUploadTimes) {
    }

    public record QualityData(@JsonProperty("minListing") ScopedListing minListing,
                              @JsonProperty("recentPurchase") ScopedPurchase recentPurchase,
                              @JsonProperty("averageSalePrice") ScopedAverage averageSalePrice,
                              @JsonProperty("dailySaleVelocity") ScopedVelocity dailySaleVelocity) {
    }

    public record ScopedListing(@JsonProperty("world") PriceEntry world,
                                @JsonProperty("dc") PriceEntry dc,
                                @JsonProperty("region") PriceEntry region) {
    }

    public record ScopedPurchase(@JsonProperty("world") PurchaseEntry world,
                                 @JsonProperty("dc") PurchaseEntry dc,
                                 @JsonProperty("region") PurchaseEntry region) {
    }

    public record ScopedAverage(@JsonProperty("world") AverageEntry world,
                                @JsonProperty("dc") AverageEntry dc,
                                @JsonProperty("region") AverageEntry region) {
    }

    public record ScopedVelocity(@JsonProperty("world") VelocityEntry world,
                                 @JsonProperty("dc") VelocityEntry dc,
                                 @JsonProperty("region") VelocityEntry region) {
    }

    public record PriceEntry(@JsonProperty("price") Long price,
                             @JsonProperty("worldId") Integer worldId) {
    }

    public record PurchaseEntry(@JsonProperty("price") Long price,
                                @JsonProperty("timestamp") Long timestamp,
                                @JsonProperty("worldId") Integer worldId) {
    }

    public record AverageEntry(@JsonProperty("price") Double price,
                               @JsonProperty("worldId") Integer worldId) {
    }

    public record VelocityEntry(@JsonProperty("quantity") Double quantity,
                                @JsonProperty("worldId") Integer worldId) {
    }

    public record WorldUploadTime(@JsonProperty("worldId") int worldId,
                                  @JsonProperty("timestamp") long timestamp) {
    }
}