package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder
public record SavedCraftRecipeRequest(@NonNull
                                      UUID recipeId,

                                      @Min(value = 1,message = "There must be at least 1 recipe")
                                      @Max(value = 999,message = "There must be less than 999 recipes")
                                      int quantity) {
}
