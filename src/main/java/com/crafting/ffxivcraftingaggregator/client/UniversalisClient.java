package com.crafting.ffxivcraftingaggregator.client;

import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisDataCenter;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPriceResponse;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisWorld;

import java.util.List;
import java.util.Map;

public interface UniversalisClient {
    UniversalisPrices getPrices(List<Integer> itemXivapiIds, String worldOrDc);
    List<UniversalisWorld> getWorlds();
    List<UniversalisDataCenter> getDataCenters();

}
