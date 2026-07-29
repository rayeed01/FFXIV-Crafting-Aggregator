package com.crafting.ffxivcraftingaggregator.client;

import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPriceResponse;

import java.util.List;
import java.util.Map;

public interface UniversalisCXlient {
    Map<Integer, UniversalisPriceResponse> getPrices(List<Integer> itemXivapiIds, String worldOrDc);
}
