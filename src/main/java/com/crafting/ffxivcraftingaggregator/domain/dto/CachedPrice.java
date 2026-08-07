package com.crafting.ffxivcraftingaggregator.domain.dto;

import com.crafting.ffxivcraftingaggregator.client.dto.ItemPrice;

public record CachedPrice(Status status,
                          Long minPrice,
                          Long minPriceNq,
                          Long minPriceHq,
                          Integer cheapestWorldId) {

    public boolean isBuyable() {
        return status == Status.PRICED && minPrice != null;
    }

    public enum Status {
        PRICED,
        UNLISTED,
        UNRESOLVED
    }

    public static CachedPrice priced(ItemPrice price) {
        return new CachedPrice(
                Status.PRICED,
                price.minPrice(),
                price.minPriceNq(),
                price.minPriceHq(),
                price.cheapestWorldId());
    }

    public static CachedPrice unlisted() {
        return new CachedPrice(Status.UNLISTED,
                null,
                null,
                null,
                null);
    }

    public static CachedPrice unresolved() {
        return new CachedPrice(Status.UNRESOLVED,
                null,
                null,
                null,
                null);
    }
}