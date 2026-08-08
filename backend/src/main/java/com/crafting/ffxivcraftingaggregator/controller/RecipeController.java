package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeSummaryDto;
import com.crafting.ffxivcraftingaggregator.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public read access to the recipe catalogue.
 *
 * <p>Unauthenticated for the same reasons as {@link ItemController}.
 */
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * Full recipe, including its result item and every ingredient.
     *
     * @return 200 with the recipe, 404 if unknown, 400 if the id is not a UUID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeDto> getRecipeById(@PathVariable UUID id){
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    /**
     * Searches by result item name, or lists every recipe for one craft type.
     *
     * <p>The two parameters are alternatives rather than filters that combine: {@code job} takes
     * precedence and {@code search} is ignored when both are supplied.
     *
     * @param search substring of the result item's name
     * @param job    an XIVAPI CraftType such as "Smithing", not a job name such as "Blacksmith"
     * @return 200 with summaries, possibly empty
     */
    @GetMapping
    public ResponseEntity<List<RecipeSummaryDto>> searchRecipes(@RequestParam(required = false) String search,
                                                                @RequestParam(required = false) String job){
        if(job != null){
            return ResponseEntity.ok(recipeService.getRecipeByJob(job));
        }
        return ResponseEntity.ok(recipeService.searchRecipes(search));
    }
}
