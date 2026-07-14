package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SavedCraftSummaryDto(UUID id,
                                  String dataCenter,
                                  String world,
                                  String notes,
                                  String title,
                                  int recipeCount,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
}
