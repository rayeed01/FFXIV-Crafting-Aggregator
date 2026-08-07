package com.crafting.ffxivcraftingaggregator.client.dto;

public record ItemPrice(int itemXivapiId,
                        Long minPrice,
                        Long minPriceNq,
                        Long minPriceHq,
                        Integer cheapestWorldId) {

    public boolean hasListing() {
        return minPrice != null;
    }

    public static ItemPrice unlisted(int itemXivapiId) {
        return new ItemPrice(itemXivapiId,
                null,
                null,
                null,
                null);
    }
}