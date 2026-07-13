package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraftRecipes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SavedCraftRecipesRepository extends JpaRepository<SavedCraftRecipes, UUID> {
}
