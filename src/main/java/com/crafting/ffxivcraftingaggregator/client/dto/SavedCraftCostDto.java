package com.crafting.ffxivcraftingaggregator.client.dto;

import com.crafting.ffxivcraftingaggregator.domain.dto.CraftCostNode;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SavedCraftCostDto(UUID savedCraftId,
                                String title,
                                String scope,
                                Long totalCraftCost,
                                Long totalBuyCost,
                                Long savings,
                                List<String> unobtainableItems,
                                List<CraftCostNode> items) {
}
