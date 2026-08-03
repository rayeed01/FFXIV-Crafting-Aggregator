package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;

import java.util.List;

public interface MarketPriceService {
    UniversalisPrices getPrices(List<Integer> itemXivapiIds, String scope);
    void exictScope(String scope);
}
