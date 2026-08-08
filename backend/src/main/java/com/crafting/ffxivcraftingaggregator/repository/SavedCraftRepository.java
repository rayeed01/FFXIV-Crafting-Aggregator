package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraft;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Crafting lists.
 *
 * <p>Both queries declare entity graphs, at different depths. A saved craft is a chain of
 * associations - list to lines to recipe to items - and without a fetch graph rendering one list
 * of ten recipes issues dozens of follow-up selects.
 */
@NullMarked
@Repository
public interface SavedCraftRepository extends JpaRepository<SavedCraft, UUID> {

    /**
     * One list, fetched deeply enough to serve the detail view and costing: lines, recipes, result
     * items and every ingredient.
     */
    @EntityGraph(attributePaths = {
            "savedCraftRecipes",
            "savedCraftRecipes.recipe",
            "savedCraftRecipes.recipe.resultItem",
            "savedCraftRecipes.recipe.recipeIngredients",
            "savedCraftRecipes.recipe.recipeIngredients.item"
    })
    @Override
    Optional<SavedCraft> findById(UUID id);

    /**
     * Every list for one user, fetched shallowly - lines and result items, but not ingredients.
     *
     * <p>The index only shows names and counts, and pulling every ingredient of every recipe of
     * every list to render that would be far more data than the page uses.
     */
    @EntityGraph(attributePaths = {
            "savedCraftRecipes",
            "savedCraftRecipes.recipe",
            "savedCraftRecipes.recipe.resultItem"
    })
    List<SavedCraft> findByUserId(UUID userId);
}
