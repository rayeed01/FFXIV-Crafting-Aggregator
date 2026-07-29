package com.crafting.ffxivcraftingaggregator.client.dto;

import java.util.*;

public record UniversalisPrices(Map<Integer, UniversalisPriceResponse> prices,
                                Set<Integer> unresolved) {

    public UniversalisPrices{
        prices = (prices == null) ? Map.of() : Map.copyOf(prices);
        unresolved = (unresolved == null) ? Set.of() : Set.copyOf(unresolved);
    }

    public static UniversalisPrices empty(){
        return new UniversalisPrices(Map.of(),Set.of());
    }

    public UniversalisPrices merge(UniversalisPrices other){
        Map<Integer,UniversalisPriceResponse> mergedPrices = new HashMap<>(this.prices);
        mergedPrices.putAll(other.prices);

        Set<Integer> mergedUnresolved = new HashSet<>(this.unresolved);
        mergedUnresolved.addAll(other.unresolved);

        return new UniversalisPrices(mergedPrices,mergedUnresolved);
    }

    public boolean isUnresolved(int itemXivapiId){
        return unresolved.contains(itemXivapiId);
    }

    public Optional<Long> minPriceFor(int itemXivapiId){
        return Optional.ofNullable(prices.get(itemXivapiId))
                .filter(UniversalisPriceResponse::hasData)
                .map(UniversalisPriceResponse::minPrice);
    }
}
