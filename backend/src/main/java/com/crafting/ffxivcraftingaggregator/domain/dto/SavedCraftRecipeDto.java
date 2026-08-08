package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

/**
 * One line of a crafting list: a recipe and how many times it is to be made.
 */
@Builder
public record SavedCraftRecipeDto(RecipeDto recipe,
                                  int quantity) {
}
