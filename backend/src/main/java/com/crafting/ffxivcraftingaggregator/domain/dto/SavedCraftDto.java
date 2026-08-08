package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A crafting list with its full contents.
 *
 * <p>{@code world} is nullable: a list priced across a whole data center has no single world.
 * {@code priceScope} is the resolved answer - the world when one is set, the data center
 * otherwise - so clients need not re-implement that rule.
 */
@Builder
public record SavedCraftDto(UUID id,
                            String dataCenter,
                            String world,
                            String priceScope,
                            String notes,
                            String title,
                            List<SavedCraftRecipeDto> recipes,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
}
