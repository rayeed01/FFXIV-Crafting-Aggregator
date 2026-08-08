package com.crafting.ffxivcraftingaggregator.mapper.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeMaterialsDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;
import com.crafting.ffxivcraftingaggregator.domain.entity.RecipeMaterials;
import com.crafting.ffxivcraftingaggregator.mapper.ItemMapper;
import com.crafting.ffxivcraftingaggregator.mapper.RecipeMapper;
import com.crafting.ffxivcraftingaggregator.mapper.RecipeMaterialsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds both recipe shapes.
 *
 * <p>The summary form flattens the result item down to a name and icon, which is all a list view
 * reads, and skips ingredients entirely.
 */
@Component
@RequiredArgsConstructor
public class RecipeMapperImpl implements RecipeMapper {

    private final ItemMapper itemMapper;
    private final RecipeMaterialsMapper recipeMaterialsMapper;

    @Override
    public RecipeDto toDto(Recipe recipe) {
        List<RecipeMaterialsDto> materials = recipe.getRecipeIngredients().stream()
                .map(recipeMaterialsMapper::toDto)
                .toList();

        return RecipeDto.builder()
                .id(recipe.getId())
                .xivapiId(recipe.getXivapiId())
                .resultQuantity(recipe.getResultQuantity())
                .job(recipe.getJob())
                .level(recipe.getLevel())
                .resultItem(itemMapper.toDto(recipe.getResultItem()))
                .materials(materials)
                .build();
    }

    @Override
    public RecipeSummaryDto toSummaryDto(Recipe recipe) {
        return RecipeSummaryDto.builder()
                .id(recipe.getId())
                .level(recipe.getLevel())
                .job(recipe.getJob())
                .resultItemName(recipe.getResultItem().getName())
                .resultItemIconUrl(recipe.getResultItem().getIconUrl())
                .build();
    }
}
