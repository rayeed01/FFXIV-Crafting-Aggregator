package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A crafting list without its contents, for the index view.
 *
 * <p>Carries {@code recipeCount} in place of the recipes themselves, so listing every list does
 * not load every recipe. Fetch the full list when the contents are actually needed.
 */
@Builder
public record SavedCraftSummaryDto(UUID id,
                                  String dataCenter,
                                  String world,
                                  String priceScope,
                                  String notes,
                                  String title,
                                  int recipeCount,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
}
