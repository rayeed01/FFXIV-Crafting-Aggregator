package com.crafting.ffxivcraftingaggregator.client;

import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisDataCenter;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisWorld;

import java.util.List;

/**
 * Outbound calls to Universalis for market prices and the server list.
 *
 * <p>Requests are chunked to the upstream item cap and the chunks recombined, so callers can ask
 * about any number of items without knowing that limit exists.
 */
public interface UniversalisClient {
    UniversalisPrices getPrices(List<Integer> itemXivapiIds, String worldOrDc);
    List<UniversalisWorld> getWorlds();
    List<UniversalisDataCenter> getDataCenters();

}
