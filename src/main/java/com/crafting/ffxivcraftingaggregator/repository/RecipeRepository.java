package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    @EntityGraph(attributePaths = {"resultItem", "recipeIngredients", "recipeIngredients.item"})
    @Override
    Optional<Recipe> findById(UUID id);

    @EntityGraph(attributePaths = {"resultItem"})
    List<Recipe> findByResultItem_NameContainingIgnoreCase(String name);

    @EntityGraph(attributePaths = {"resultItem"})
    List<Recipe> findByJobIgnoreCase(String job);

    Optional<Recipe> findByResultItem(Item item);

    Optional<Recipe> findByXivapiId(int xivapiId);

    @Transactional
    @Modifying
    @Query("UPDATE Item i SET i.canBeCrafted = true where i.id IN (SELECT r.resultItem.id from Recipe r)")
    void markResultItemsCraftable();


}
