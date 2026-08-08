package com.crafting.ffxivcraftingaggregator.client.dto;

/**
 * Cheapest current listing for an item within one scope.
 *
 * <p>NQ and HQ are kept apart rather than reduced to a single figure: a recipe that demands HQ
 * materials cannot be satisfied by an NQ listing, and HQ gear is frequently what a buyer actually
 * wants. {@code minPrice} remains the cheaper of the two for callers that do not care.
 *
 * <p>Each quality carries its own world id. A single shared id was wrong whenever the two
 * qualities were cheapest on different worlds - it named the world for whichever quality won,
 * sending anyone buying the other one to the wrong place.
 */
public record ItemPrice(int itemXivapiId,
                        Long minPrice,
                        Long minPriceNq,
                        Long minPriceHq,
                        Integer cheapestWorldId,
                        Integer nqWorldId,
                        Integer hqWorldId) {

    public boolean hasListing() {
        return minPrice != null;
    }

    public static ItemPrice unlisted(int itemXivapiId) {
        return new ItemPrice(itemXivapiId,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
