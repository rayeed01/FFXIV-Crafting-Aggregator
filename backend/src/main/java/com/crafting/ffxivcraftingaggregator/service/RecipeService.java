package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read access to the recipe catalogue imported from XIVAPI.
 *
 * <p>Search returns {@link RecipeSummaryDto} rather than the full recipe: a list view needs a name,
 * job and level, and fetching every recipe's ingredients to render it would be wasteful.
 */
public interface RecipeService {

    /**
     * Full recipe including its result item and every ingredient.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.RecipeNotFoundException if no such recipe
     */
    RecipeDto getRecipeById(UUID id);

    /**
     * Case-insensitive substring match on the result item's name.
     *
     * <p>Capped for the same reason as item search. A blank query yields an empty list.
     */
    List<RecipeSummaryDto> searchRecipes(String query);

    /**
     * Every recipe for one craft type.
     *
     * @param job an XIVAPI CraftType name such as "Smithing", not a job name such as "Blacksmith"
     */
    List<RecipeSummaryDto> getRecipeByJob(String job);

    /** The recipe producing this item, if it is craftable at all. */
    Optional<Recipe> findRecipeForItem(Item item);

    /** Looks a recipe up by its XIVAPI id, used by the sync to detect rows it has already imported. */
    Optional<Recipe> getRecipeByXivapiId(int xivapiId);
}
