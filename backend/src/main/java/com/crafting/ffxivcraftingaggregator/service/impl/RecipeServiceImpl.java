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
import org.springframework.data.domain.PageRequest;
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

    /** Matches the item search cap, so a merged result set is not lopsided. */
    private static final int SEARCH_LIMIT = 50;

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
                .searchByRelevance(escapeLikeWildcards(query.trim()), PageRequest.of(0, SEARCH_LIMIT))
                .stream()
                .map(recipeMapper::toSummaryDto)
                .toList();
    }

    /**
     * Neutralises the characters LIKE treats as wildcards.
     *
     * <p>Spring Data's derived Containing queries escape these automatically; an explicit LIKE
     * does not. Without this a search for "%" matches every recipe, and "_" matches any single
     * character.
     *
     * <p>The backslash is escaped first, otherwise it would re-escape the escapes added after it.
     * The queries declare {@code ESCAPE '\'} to match.
     */
    private static String escapeLikeWildcards(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
