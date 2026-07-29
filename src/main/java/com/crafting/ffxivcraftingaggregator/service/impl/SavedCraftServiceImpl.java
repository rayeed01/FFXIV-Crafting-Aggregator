package com.crafting.ffxivcraftingaggregator.service.impl;

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
import com.crafting.ffxivcraftingaggregator.service.SavedCraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedCraftServiceImpl implements SavedCraftService {

    private final SavedCraftRepository savedCraftRepository;
    private final SavedCraftMapper savedCraftMapper;
    private final UserRepository userRepository;
    private  final RecipeRepository recipeRepository;

    @Transactional
    @Override
    public SavedCraftDto createSavedCraftRequest(UUID userId, CreateSavedCraftRequest request) {
        User user = userRepository.getReferenceById(userId);

        List<Recipe> recipes = recipeRepository.findAllById(request.recipeIds());
        validateAllRecipesFound(request.recipeIds(), recipes);

        SavedCraft savedCraft = SavedCraft.builder()
                .user(user)
                .title(request.title())
                .dataCenter(request.dataCenter())
                .world(request.world())
                .notes(request.notes())
                .build();

        recipes.forEach(recipe -> savedCraft.getSavedCraftRecipes().add(
                SavedCraftRecipes.builder()
                        .savedCraft(savedCraft)
                        .recipe(recipe)
                        .build()
        ));

        SavedCraft saved = savedCraftRepository.save(savedCraft);
        return savedCraftMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public SavedCraftDto getSavedCraft(UUID userId, UUID savedCraftId) {
        SavedCraft savedCraft = findOwnedSavedCraftOrThrow(savedCraftId,userId);
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

        savedCraft.setTitle(request.title());
        savedCraft.setDataCenter(request.dataCenter());
        savedCraft.setWorld(request.world());
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

        List<Recipe> recipes = recipeRepository.findAllById(request.recipeIds());
        validateAllRecipesFound(request.recipeIds(),recipes);

        Set<UUID> existingRecipeIds = savedCraft.getSavedCraftRecipes().stream()
                .map(scr -> scr.getRecipe().getId())
                .collect(Collectors.toSet());

        recipes.stream()
                .filter(recipe -> !existingRecipeIds.contains(recipe.getId()))
                .forEach(recipe -> savedCraft.getSavedCraftRecipes().add(
                        SavedCraftRecipes.builder()
                                .savedCraft(savedCraft)
                                .recipe(recipe)
                                .build()
                ));

        SavedCraft saved = savedCraftRepository.save(savedCraft);
        return savedCraftMapper.toDto(saved);
    }

    @Transactional
    @Override
    public SavedCraftDto removeRecipes(UUID userId, UUID savedCraftId, AddRecipeRequest request) {
        SavedCraft savedCraft = findOwnedSavedCraftOrThrow(userId,savedCraftId);

        Set<UUID> idsToRemove = Set.copyOf(request.recipeIds());

        savedCraft.getSavedCraftRecipes()
                .removeIf(scr -> idsToRemove.contains(scr.getRecipe().getId()));

        SavedCraft saved = savedCraftRepository.save(savedCraft);
        return savedCraftMapper.toDto(saved);
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
}
