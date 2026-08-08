package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * One ingredient line of a recipe: an item and how many of it a single craft consumes.
 *
 * <p>The quantity is per craft, not per requested unit.
 */
@Builder
public record RecipeMaterialsDto(UUID id,
                                 ItemDto item,
                                 int quantity) {
}
