package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;

/**
 * Converts a {@link Recipe} entity into the two shapes the API returns.
 *
 * <p>The summary form exists so list views need not carry every ingredient of every match.
 */
public interface RecipeMapper {
    RecipeDto toDto(Recipe recipe);
    RecipeSummaryDto toSummaryDto(Recipe recipe);
}
