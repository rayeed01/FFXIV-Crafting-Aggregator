package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.CachedPrice;

import java.util.List;
import java.util.Map;

/**
 * Market prices, served from Redis and fetched from Universalis on a miss.
 *
 * <p>The cache is what makes recursive costing viable: a deep recipe tree asks about the same
 * crystals and shards over and over, and every distinct scope multiplies that again.
 *
 * <p>Entries are quality-agnostic - a single entry carries both the NQ and HQ price - so asking
 * for a different quality never invalidates anything or costs an extra upstream call.
 */
public interface MarketPriceService {

    /**
     * Prices a batch of items, in one upstream request for whatever is not already cached.
     *
     * <p>Every requested id appears in the result. An id Universalis does not recognise, or has no
     * listing for, is present with a status saying so rather than being silently absent - the
     * caller must be able to tell "no listing" from "never asked".
     *
     * @param itemXivapiIds game item ids; duplicates are collapsed
     * @param scope         canonical world or data center name
     */
    Map<Integer, CachedPrice> getPrices(List<Integer> itemXivapiIds, String scope);

    /** Drops every cached price for one scope, for when stale data must be discarded early. */
    void evictScope(String scope);
}
