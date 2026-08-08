package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.RecipeMaterials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Ingredient rows pairing a recipe with an item and a quantity.
 *
 * <p>No query methods: ingredients are always reached through their recipe, which fetches them by
 * entity graph. Written by the XIVAPI sync and otherwise read-only.
 */
public interface RecipeMaterialsRepository extends JpaRepository<RecipeMaterials, UUID> {

}
