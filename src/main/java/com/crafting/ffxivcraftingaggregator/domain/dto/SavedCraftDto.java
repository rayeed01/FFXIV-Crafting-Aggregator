package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record SavedCraftDto(UUID id,
                            String dataCenter,
                            String world,
                            String notes,
                            String title,
                            List<RecipeDto> recipes,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
}
