package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Recipes to add to a list, or new quantities for ones already in it.
 *
 * <p>This upserts, which is what lets a client change a quantity by re-sending the same recipe id.
 * The list must be non-empty, since an empty request would be a silent no-op.
 */
@Builder
public record AddRecipeRequest(@NotEmpty(message = "At least one recipe must be provided")
                               List<@Valid SavedCraftRecipeRequest> recipes) {
}
