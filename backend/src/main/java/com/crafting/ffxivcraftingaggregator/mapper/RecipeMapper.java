package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;

public interface RecipeMapper {
    RecipeDto toDto(Recipe recipe);
    RecipeSummaryDto toSummaryDto(Recipe recipe);
}
