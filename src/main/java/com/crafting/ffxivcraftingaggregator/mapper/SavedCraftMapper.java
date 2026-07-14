package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.SavedCraftDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.SavedCraftSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraft;

public interface SavedCraftMapper {
    SavedCraftDto toDto(SavedCraft savedCraft);
    SavedCraftSummaryDto toSummaryDto(SavedCraft savedCraft);
}
