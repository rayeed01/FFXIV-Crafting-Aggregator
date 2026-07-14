package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AddRecipeRequest(@NotEmpty(message = "At least one recipe id must be provided")
                               List<UUID> recipeIds) {
}
