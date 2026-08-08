package com.crafting.ffxivcraftingaggregator.client.dto;

import com.crafting.ffxivcraftingaggregator.domain.dto.CraftCostNode;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * The costed result of a whole crafting list.
 *
 * <p>Totals are null rather than partial when anything is unobtainable: a sum that quietly omits
 * items reads as the full cost of the list and is not. {@code unobtainableItems} names what was
 * left out.
 *
 * <p>{@code totalBuyCost} and {@code savings} additionally require every item to be purchasable,
 * not merely obtainable - one unbuyable item and the comparison has no honest denominator, so
 * both stay null while the craft total still stands.
 */
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
