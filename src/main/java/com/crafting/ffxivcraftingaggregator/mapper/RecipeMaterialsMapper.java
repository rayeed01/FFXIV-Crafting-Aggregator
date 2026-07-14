package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeMaterialsDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.RecipeMaterials;

public interface RecipeMaterialsMapper {
    RecipeMaterialsDto toDto(RecipeMaterials recipeMaterials);
}
