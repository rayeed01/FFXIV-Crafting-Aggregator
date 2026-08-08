package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.client.dto.SavedCraftCostDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.*;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;
import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraft;
import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraftRecipes;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;
import com.crafting.ffxivcraftingaggregator.exception.RecipeNotFoundException;
import com.crafting.ffxivcraftingaggregator.exception.SavedCraftNotFoundException;
import com.crafting.ffxivcraftingaggregator.mapper.SavedCraftMapper;
import com.crafting.ffxivcraftingaggregator.repository.RecipeRepository;
import com.crafting.ffxivcraftingaggregator.repository.SavedCraftRepository;
import com.crafting.ffxivcraftingaggregator.repository.UserRepository;
import com.crafting.ffxivcraftingaggregator.service.CraftCostService;
import com.crafting.ffxivcraftingaggregator.service.SavedCraftService;
import com.crafting.ffxivcraftingaggregator.service.WorldRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedCraftServiceImpl implements SavedCraftService {

    private final SavedCraftRepository savedCraftRepository;
    private final SavedCraftMapper savedCraftMapper;
    private final UserRepository userRepository;
    private  final RecipeRepository recipeRepository;
    private final WorldRegistry worldRegistry;
    private final CraftCostService craftCostService;

    @Transactional
    @Override
    public SavedCraftDto createSavedCraftRequest(UUID userId, CreateSavedCraftRequest request) {
        User user = userRepository.getReferenceById(userId);

        ResolvedScope scope = resolveScope(request.dataCenter(), request.world());

        List<UUID> recipeIds = request.recipes().stream()
                .map(SavedCraftRecipeRequest::recipeId)
                .toList();

        List<Recipe> recipes = recipeRepository.findAllById(recipeIds);
        validateAllRecipesFound(recipeIds, recipes);

        SavedCraft savedCraft = SavedCraft.builder()
                .user(user)
                .title(request.title())
                .dataCenter(scope.dataCenter())
                .world(scope.world())
                .notes(request.notes())
                .build();

        Map<UUID,Recipe> recipesById = recipes.stream()
                        .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        request.recipes().forEach(line -> savedCraft.getSavedCraftRecipes().add(
                SavedCraftRecipes.builder()
                        .savedCraft(savedCraft)
                        .recipe(recipesById.get(line.recipeId()))
                        .quantity(line.quantity())
                        .build()
        ));

        SavedCraft saved = savedCraftRepository.save(savedCraft);
        return savedCraftMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public SavedCraftDto getSavedCraft(UUID userId, UUID savedCraftId) {
        SavedCraft savedCraft = findOwnedSavedCraftOrThrow(userId, savedCraftId);
        return savedCraftMapper.toDto(savedCraft);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SavedCraftSummaryDto> getUserSavedCraft(UUID userId) {
        return savedCraftRepository.findByUserId(userId).stream()
                .map(savedCraftMapper::toSummaryDto)
                .toList();
    }

    @Transactional
    @Override
    public SavedCraftDto updateSavedCraft(UUID userId, UUID savedCraftId, UpdateSavedCraftRequest request) {
        SavedCraft savedCraft = findOwnedSavedCraftOrThrow(userId, savedCraftId);

        ResolvedScope scope = resolveScope(request.dataCenter(), request.world());

        savedCraft.setTitle(request.title());
        savedCraft.setDataCenter(scope.dataCenter);
        savedCraft.setWorld(scope.world);
        savedCraft.setNotes(request.notes());

        SavedCraft saved = savedCraftRepository.save(savedCraft);
        return savedCraftMapper.toDto(saved);
    }

    @Transactional
    @Override
    public void deleteSavedCraft(UUID userId, UUID savedCraftId) {
        SavedCraft savedCraft = findOwnedSavedCraftOrThrow(userId,savedCraftId);

        savedCraftRepository.delete(savedCraft);

    }

    @Transactional
    @Override
    public SavedCraftDto addRecipes(UUID userId, UUID savedCraftId, AddRecipeRequest request) {
        SavedCraft savedCraft = findOwnedSavedCraftOrThrow(userId,savedCraftId);

        List<UUID> recipeIds = request.recipes().stream()
                .map(SavedCraftRecipeRequest::recipeId)
                .toList();

        List<Recipe> recipes = recipeRepository.findAllById(recipeIds);
        validateAllRecipesFound(recipeIds,recipes);

        Map<UUID, Recipe> recipesById = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        Map<UUID, SavedCraftRecipes> existingByRecipeId = savedCraft.getSavedCraftRecipes().stream()
                .collect(Collectors.toMap(scr -> scr.getRecipe().getId(), Function.identity()));

        for(SavedCraftRecipeRequest line : request.recipes()) {
            SavedCraftRecipes existing = existingByRecipeId.get(line.recipeId());

            if(existing != null) {
                existing.setQuantity(line.quantity());
            } else{
                savedCraft.getSavedCraftRecipes().add(
                        SavedCraftRecipes.builder()
                                .savedCraft(savedCraft)
                                .recipe(recipesById.get(line.recipeId()))
                                .quantity(line.quantity())
                                .build());
            }
        }

        SavedCraft saved = savedCraftRepository.save(savedCraft);
        return savedCraftMapper.toDto(saved);
    }

    @Transactional
    @Override
    public SavedCraftDto removeRecipes(UUID userId, UUID savedCraftId, RemoveRecipeRequest request) {
        SavedCraft savedCraft = findOwnedSavedCraftOrThrow(userId,savedCraftId);

        Set<UUID> idsToRemove = Set.copyOf(request.recipeIds());

        savedCraft.getSavedCraftRecipes()
                .removeIf(scr -> idsToRemove.contains(scr.getRecipe().getId()));

        SavedCraft saved = savedCraftRepository.save(savedCraft);
        return savedCraftMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public SavedCraftCostDto calculateCost(UUID userId, UUID savedCraftId) {
        SavedCraft savedCraft = findOwnedSavedCraftOrThrow(userId, savedCraftId);

        if (savedCraft.getSavedCraftRecipes().isEmpty()) {
            return SavedCraftCostDto.builder()
                    .savedCraftId(savedCraft.getId())
                    .title(savedCraft.getTitle())
                    .scope(savedCraft.getPriceScope())
                    .totalCraftCost(0L)
                    .totalBuyCost(0L)
                    .savings(0L)
                    .unobtainableItems(List.of())
                    .items(List.of())
                    .build();
        }

        // Two lines can point at different recipes producing the same item - alternative jobs, or
        // an expert variant. Merging the quantities means the item is priced once for the combined
        // total rather than twice at partial quantities, which matters because recipe yield makes
        // cost non-linear: 2 + 2 of a yield-3 recipe is two crafts, but 4 is only two as well.
        Map<Integer, Integer> itemQuantities = new LinkedHashMap<>();
        for (SavedCraftRecipes line : savedCraft.getSavedCraftRecipes()) {
            int itemId = line.getRecipe().getResultItem().getXivapiId();
            itemQuantities.merge(itemId, line.getQuantity(), Integer::sum);
        }

        List<CraftCostNode> nodes =
                craftCostService.calculateAll(itemQuantities, savedCraft.getPriceScope());

        return summarise(savedCraft, nodes);
    }

    private SavedCraftCostDto summarise(SavedCraft savedCraft, List<CraftCostNode> nodes) {

        List<String> unobtainable = nodes.stream()
                .filter(node -> node.effectiveCost() == null)
                .map(CraftCostNode::itemName)
                .toList();

        Long totalCraftCost = null;
        Long totalBuyCost = null;
        Long savings = null;

        if (unobtainable.isEmpty()) {
            totalCraftCost = nodes.stream()
                    .mapToLong(CraftCostNode::effectiveCost)
                    .sum();

            // Buying outright is only a meaningful comparison if every item CAN be bought. One
            // unbuyable item and the "what you saved" figure has no honest denominator.
            boolean allBuyable = nodes.stream().allMatch(node -> node.buyCost() != null);

            if (allBuyable) {
                totalBuyCost = nodes.stream()
                        .mapToLong(CraftCostNode::buyCost)
                        .sum();
                savings = totalBuyCost - totalCraftCost;
            }
        }

        return SavedCraftCostDto.builder()
                .savedCraftId(savedCraft.getId())
                .title(savedCraft.getTitle())
                .scope(savedCraft.getPriceScope())
                .totalCraftCost(totalCraftCost)
                .totalBuyCost(totalBuyCost)
                .savings(savings)
                .unobtainableItems(unobtainable)
                .items(nodes)
                .build();
    }

    private ResolvedScope resolveScope(String requestedDatacenter, String requestedWorld){
        String dataCenter = worldRegistry.canonicalDataCenterName(requestedDatacenter);
        if(requestedWorld == null || requestedWorld.isBlank()){
            return new ResolvedScope(dataCenter, null);
        }

        String world = worldRegistry.canonicalWorldName(requestedWorld);
        worldRegistry.validateWorldBelongsToDataCenter(world,dataCenter);

        return new ResolvedScope(dataCenter,world);
    }

    private SavedCraft findOwnedSavedCraftOrThrow(UUID userId, UUID savedCraftId) {
        SavedCraft savedCraft = savedCraftRepository.findById(savedCraftId)
                .orElseThrow(() -> new SavedCraftNotFoundException("List not found"));

        if(!savedCraft.getUser().getId().equals(userId)){
            throw new SavedCraftNotFoundException("List not found");
        }

        return savedCraft;
    }

    private void validateAllRecipesFound(List<UUID> requestedIds, List<Recipe> foundRecipes){
        if(foundRecipes.size() != requestedIds.size()){
            throw new RecipeNotFoundException("One or more recipes not found");
        }
    }

    private record ResolvedScope(String dataCenter, String world){}
}
