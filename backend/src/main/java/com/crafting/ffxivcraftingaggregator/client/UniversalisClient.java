package com.crafting.ffxivcraftingaggregator.client;

import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisDataCenter;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisWorld;

import java.util.List;

public interface UniversalisClient {
    UniversalisPrices getPrices(List<Integer> itemXivapiIds, String worldOrDc);
    List<UniversalisWorld> getWorlds();
    List<UniversalisDataCenter> getDataCenters();

}
