package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraft;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
@Repository
public interface SavedCraftRepository extends JpaRepository<SavedCraft, UUID> {
    @EntityGraph(attributePaths = {
            "savedCraftRecipes",
            "savedCraftRecipes.recipe",
            "savedCraftRecipes.recipe.resultItem",
            "savedCraftRecipes.recipe.recipeIngredients",
            "savedCraftRecipes.recipe.recipeIngredients.item"
    })
    @Override
    Optional<SavedCraft> findById(UUID id);

    @EntityGraph(attributePaths = {
            "savedCraftRecipes",
            "savedCraftRecipes.recipe",
            "savedCraftRecipes.recipe.resultItem"
    })
    List<SavedCraft> findByUserId(UUID userId);
}
