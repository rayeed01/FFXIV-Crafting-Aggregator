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

                            /** Both qualities, so the client can show the one not chosen. */
                            Long buyCostNq,
                            Long buyCostHq,

                            /** Which quality {@code buyCost} came from; null when unlisted. */
                            Quality buyQuality,

                            /**
                             * Craft type and level of this item's recipe, for display alongside
                             * the row. Both absent when the item has no recipe - a raw material
                             * is nobody's craft.
                             */
                            String job,
                            Integer level,

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
