package com.crafting.ffxivcraftingaggregator.client.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The result of one price fetch: what was priced, and what could not be.
 *
 * <p>Unresolved ids are tracked separately rather than being left absent, so a caller can tell an
 * item Universalis does not know from one that simply has no listing today. The two deserve
 * very different cache lifetimes.
 */
public record UniversalisPrices(Map<Integer, ItemPrice> prices,
                                Set<Integer> unresolved) {

    public UniversalisPrices {
        prices = (prices == null) ? Map.of() : Map.copyOf(new HashMap<>(prices));
        unresolved = (unresolved == null) ? Set.of() : Set.copyOf(new HashSet<>(unresolved));
    }

    public static UniversalisPrices empty() {
        return new UniversalisPrices(Map.of(), Set.of());
    }

    public UniversalisPrices merge(UniversalisPrices other) {
        Map<Integer, ItemPrice> mergedPrices = new HashMap<>(this.prices);
        mergedPrices.putAll(other.prices);

        Set<Integer> mergedUnresolved = new HashSet<>(this.unresolved);
        mergedUnresolved.addAll(other.unresolved);

        return new UniversalisPrices(mergedPrices, mergedUnresolved);
    }

    public boolean isUnresolved(int itemXivapiId) {
        return unresolved.contains(itemXivapiId);
    }
}