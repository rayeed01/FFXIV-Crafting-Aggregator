package com.crafting.ffxivcraftingaggregator.domain.dto;

import com.crafting.ffxivcraftingaggregator.client.dto.ItemPrice;

/**
 * Redis-cached form of {@link ItemPrice}.
 *
 * <p>Both qualities live in a single entry keyed only by scope and item, so the cache is
 * quality-agnostic: switching the requested quality never invalidates anything and never costs
 * an extra Universalis call.
 *
 * <p>Entries written before nqWorldId/hqWorldId existed deserialise with those fields null, which
 * only suppresses the "cheapest on world X" hint until the 15-minute TTL turns them over.
 */
public record CachedPrice(Status status,
                          Long minPrice,
                          Long minPriceNq,
                          Long minPriceHq,
                          Integer cheapestWorldId,
                          Integer nqWorldId,
                          Integer hqWorldId) {

    public boolean isBuyable() {
        return status == Status.PRICED && minPrice != null;
    }

    /** Unit price for a quality preference, or null when that quality has no listing. */
    public Long priceFor(Quality quality) {
        return switch (quality) {
            case CHEAPEST -> minPrice;
            case NQ -> minPriceNq;
            case HQ -> minPriceHq;
        };
    }

    /** World the {@link #priceFor} figure came from, or null when unknown. */
    public Integer worldFor(Quality quality) {
        return switch (quality) {
            case CHEAPEST -> cheapestWorldId;
            case NQ -> nqWorldId;
            case HQ -> hqWorldId;
        };
    }

    /** Which quality {@code minPrice} actually came from, or null when nothing is listed. */
    public Quality cheapestQuality() {
        if (minPrice == null) return null;
        if (minPriceNq != null && minPrice.equals(minPriceNq)) return Quality.NQ;
        if (minPriceHq != null && minPrice.equals(minPriceHq)) return Quality.HQ;
        return null;
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
                price.cheapestWorldId(),
                price.nqWorldId(),
                price.hqWorldId());
    }

    public static CachedPrice unlisted() {
        return new CachedPrice(Status.UNLISTED, null, null, null, null, null, null);
    }

    public static CachedPrice unresolved() {
        return new CachedPrice(Status.UNRESOLVED, null, null, null, null, null, null);
    }
}
