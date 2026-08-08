package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

@Builder
public record SavedCraftRecipeDto(RecipeDto recipe,
                                  int quantity) {
}
