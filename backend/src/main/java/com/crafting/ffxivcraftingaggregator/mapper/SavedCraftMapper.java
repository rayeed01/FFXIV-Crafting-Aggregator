package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.SavedCraftDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.SavedCraftSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraft;

/**
 * Converts a {@link SavedCraft} entity into the two shapes the API returns.
 *
 * <p>Both derive the price scope from the stored data center and optional world, so clients never
 * have to re-implement that rule.
 */
public interface SavedCraftMapper {
    SavedCraftDto toDto(SavedCraft savedCraft);
    SavedCraftSummaryDto toSummaryDto(SavedCraft savedCraft);
}
