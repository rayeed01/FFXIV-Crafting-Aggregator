package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.client.dto.SavedCraftCostDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * Crafting lists: named sets of recipes priced together against one market.
 *
 * <p>Every method takes the owner's id and every method enforces it. A list belonging to someone
 * else is reported as not found rather than forbidden, because a 403 confirms the id exists and
 * lets one user enumerate another's lists.
 */
public interface SavedCraftService {

    /**
     * Creates a list. The recipe collection may be empty; recipes are usually added afterwards.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.UnknownDataCenterException if the scope is invalid
     * @throws com.crafting.ffxivcraftingaggregator.exception.RecipeNotFoundException if any recipe id is unknown
     */
    SavedCraftDto createSavedCraftRequest(UUID userId, CreateSavedCraftRequest request);

    /**
     * @throws com.crafting.ffxivcraftingaggregator.exception.SavedCraftNotFoundException
     *         if no such list, or it belongs to another user
     */
    SavedCraftDto getSavedCraft(UUID userId, UUID savedCraftId);

    /** Summaries for the list index: no recipes, only a count. */
    List<SavedCraftSummaryDto> getUserSavedCraft(UUID userId);

    /**
     * Updates title, notes and pricing scope. Contents are unaffected.
     *
     * <p>Changing the scope does not re-price anything; the next cost calculation simply uses the
     * new market.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.SavedCraftNotFoundException
     *         if no such list, or it belongs to another user
     */
    SavedCraftDto updateSavedCraft(UUID userId, UUID savedCraftId,UpdateSavedCraftRequest request);

    /**
     * @throws com.crafting.ffxivcraftingaggregator.exception.SavedCraftNotFoundException
     *         if no such list, or it belongs to another user
     */
    void deleteSavedCraft(UUID userId, UUID savedCraftId);

    /**
     * Adds recipes, or updates the quantity of ones already present.
     *
     * <p>This upserts rather than rejecting duplicates, which is what lets a client edit a
     * quantity by re-sending the same recipe id: there is deliberately no separate endpoint for it.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.RecipeNotFoundException if any recipe id is unknown
     * @throws com.crafting.ffxivcraftingaggregator.exception.SavedCraftNotFoundException
     *         if no such list, or it belongs to another user
     */
    SavedCraftDto addRecipes(UUID userId, UUID savedCraftId, AddRecipeRequest request);

    /**
     * Removes recipes. Ids not in the list are ignored rather than treated as an error.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.SavedCraftNotFoundException
     *         if no such list, or it belongs to another user
     */
    SavedCraftDto removeRecipes(UUID userId, UUID savedCraftId,RemoveRecipeRequest request);

    /**
     * Prices the whole list against its own scope.
     *
     * <p>Expensive: it fans out to Universalis for every ingredient of every recipe, which is why
     * clients trigger it explicitly rather than on every view.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.SavedCraftNotFoundException
     *         if no such list, or it belongs to another user
     */
    SavedCraftCostDto calculateCost(UUID userId, UUID savedCraftId);

}
