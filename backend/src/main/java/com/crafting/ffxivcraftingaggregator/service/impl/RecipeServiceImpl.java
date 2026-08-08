package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;
import com.crafting.ffxivcraftingaggregator.exception.RecipeNotFoundException;
import com.crafting.ffxivcraftingaggregator.mapper.RecipeMapper;
import com.crafting.ffxivcraftingaggregator.repository.RecipeRepository;
import com.crafting.ffxivcraftingaggregator.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * Recipe lookups over the synced catalogue.
 *
 * <p>Search returns summaries rather than full recipes, and is capped for the same reason as item
 * search.
 */
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    @Transactional(readOnly = true)
    @Override
    public RecipeDto getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found"));

        return recipeMapper.toDto(recipe);
    }

    @Transactional(readOnly = true)
    @Override
    public List<RecipeSummaryDto> searchRecipes(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        return recipeRepository
                .findTop50ByResultItem_NameContainingIgnoreCaseOrderByResultItem_NameAsc(query.trim()).stream()
                .map(recipeMapper::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<RecipeSummaryDto> getRecipeByJob(String job) {
        return recipeRepository.findByJobIgnoreCase(job).stream()
                .map(recipeMapper::toSummaryDto)
                .toList();
    }

    @Override
    public Optional<Recipe> findRecipeForItem(Item item) {
        return recipeRepository.findByResultItem(item);
    }

    @Override
    public Optional<Recipe> getRecipeByXivapiId(int xivapiId) {
        return recipeRepository.findByXivapiId(xivapiId);
    }
}
