package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeMaterialsDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.RecipeMaterials;

/**
 * Converts one recipe ingredient line into its API shape.
 */
public interface RecipeMaterialsMapper {
    RecipeMaterialsDto toDto(RecipeMaterials recipeMaterials);
}
