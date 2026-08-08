package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.CraftCostNode;
import com.crafting.ffxivcraftingaggregator.domain.dto.Quality;

import java.util.List;
import java.util.Map;

public interface CraftCostService {

    CraftCostNode calculate(int itemXivapiId, int quantity, String scope);

    /**
     * As above, but prices the requested item at a specific quality.
     *
     * <p>The preference applies to the root item only. Ingredients stay on the cheapest listing,
     * because an HQ result comes from crafting skill rather than from HQ materials - forcing HQ
     * inputs would inflate the craft side with a cost the crafter would not actually pay.
     */
    CraftCostNode calculate(int itemXivapiId, int quantity, String scope, Quality quality);

    List<CraftCostNode> calculateAll(Map<Integer,Integer> itemQuantities, String scope);

    List<CraftCostNode> calculateAll(Map<Integer,Integer> itemQuantities, String scope, Quality quality);
}