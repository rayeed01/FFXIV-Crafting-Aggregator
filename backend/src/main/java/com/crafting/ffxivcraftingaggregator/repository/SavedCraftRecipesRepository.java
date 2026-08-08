package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraftRecipes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Join rows pairing a saved craft with a recipe and a quantity.
 *
 * <p>No query methods: these rows are managed through their owning {@code SavedCraft}, which
 * cascades saves and removals, so they are never loaded independently. The repository exists for
 * the few places that need direct access to the entity type.
 */
public interface SavedCraftRecipesRepository extends JpaRepository<SavedCraftRecipes, UUID> {
}
