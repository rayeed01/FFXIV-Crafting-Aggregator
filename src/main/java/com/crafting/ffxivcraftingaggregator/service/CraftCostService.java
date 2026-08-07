package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.CraftCostNode;

import java.util.List;
import java.util.Map;

public interface CraftCostService {
    CraftCostNode calculate(int itemXivapiId, int quantity, String scope);
    List<CraftCostNode> calculateAll(Map<Integer,Integer> itemQuantities, String scope);
}