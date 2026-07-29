package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeService {
    RecipeDto getRecipeById(UUID id);
    List<RecipeSummaryDto> searchRecipes(String query);
    List<RecipeSummaryDto> getRecipeByJob(String job);
    Optional<Recipe> findRecipeForItem(Item item);
    Optional<Recipe> getRecipeByXivapiId(int xivapiId);
}
