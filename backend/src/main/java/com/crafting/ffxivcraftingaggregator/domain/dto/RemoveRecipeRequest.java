package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Recipe ids to remove from a list.
 *
 * <p>Ids that are not present are ignored rather than rejected, so removing something twice is
 * harmless. Carried in a body rather than the path so several can go in one request.
 */
@Builder
public record RemoveRecipeRequest(@NotEmpty(message = "At least one recipe id must be provided")
                                  List<UUID> recipeIds) {
}
