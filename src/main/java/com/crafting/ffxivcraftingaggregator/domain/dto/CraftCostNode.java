package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CraftCostNode(int itemXivapiId,
                            String itemName,
                            int quantityNeeded,
                            Long buyCost,
                            Long craftCost,
                            Long effectiveCost,
                            Decision decision,
                            int craftsRequired,
                            int surplus,
                            Integer cheapestWorldId,
                            List<CraftCostNode> ingredients) {


    public CraftCostNode {
        ingredients = (ingredients == null) ? List.of() : List.copyOf(ingredients);
    }

    public enum Decision {
        BUY,
        CRAFT,
        UNOBTAINABLE,
        CYCLE
    }

    public boolean isObtainable() {
        return effectiveCost != null;
    }
}
