package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

/**
 * A requested line for a crafting list.
 *
 * <p>Quantity is bounded below at 1 because a zero would price at 0 gil and read downstream as
 * free, and above at 999 to keep one request from expanding into an unbounded recipe tree.
 */
@Builder
public record SavedCraftRecipeRequest(@NonNull
                                      UUID recipeId,

                                      @Min(value = 1,message = "There must be at least 1 recipe")
                                      @Max(value = 999,message = "There must be less than 999 recipes")
                                      int quantity) {
}
