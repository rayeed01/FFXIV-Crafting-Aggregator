package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.client.dto.SavedCraftCostDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.*;

import java.util.List;
import java.util.UUID;

public interface SavedCraftService {
    SavedCraftDto createSavedCraftRequest(UUID userId, CreateSavedCraftRequest request);
    SavedCraftDto getSavedCraft(UUID userId, UUID savedCraftId);
    List<SavedCraftSummaryDto> getUserSavedCraft(UUID userId);
    SavedCraftDto updateSavedCraft(UUID userId, UUID savedCraftId,UpdateSavedCraftRequest request);
    void deleteSavedCraft(UUID userId, UUID savedCraftId);
    SavedCraftDto addRecipes(UUID userId, UUID savedCraftId, AddRecipeRequest request);
    SavedCraftDto removeRecipes(UUID userId, UUID savedCraftId,RemoveRecipeRequest request);
    SavedCraftCostDto calculateCost(UUID userId, UUID savedCraftId);

}
