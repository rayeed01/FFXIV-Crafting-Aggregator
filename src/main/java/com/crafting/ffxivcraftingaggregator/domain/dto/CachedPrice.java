package com.crafting.ffxivcraftingaggregator.domain.dto;

public record CachedPrice(Status status,
                          Long minPrice,
                          Long minPriceNq,
                          Long minPriceHq,
                          Long lastUploadTime) {

    public enum Status{
        PRICED,
        UNLISTED,
        UNRESOLVED
    }

    public static CachedPrice priced(Long minPrice, Long minPriceNq, Long minPriceHq, Long lastUploadTime) {
        return new CachedPrice(Status.PRICED, minPrice, minPriceNq, minPriceHq, lastUploadTime);
    }

    public static CachedPrice unlisted() {
        return new CachedPrice(Status.UNLISTED, null, null, null, null);
    }

    public static CachedPrice unresolved() {
        return new CachedPrice(Status.UNRESOLVED, null, null, null, null);
    }

    public boolean isBuyable() {
        return status == Status.PRICED && minPrice != null;
    }
}
