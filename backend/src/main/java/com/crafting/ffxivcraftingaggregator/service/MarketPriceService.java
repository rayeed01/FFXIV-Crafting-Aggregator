package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.domain.dto.CachedPrice;

import java.util.List;
import java.util.Map;

public interface MarketPriceService {
    Map<Integer, CachedPrice> getPrices(List<Integer> itemXivapiIds, String scope);
    void evictScope(String scope);
}
