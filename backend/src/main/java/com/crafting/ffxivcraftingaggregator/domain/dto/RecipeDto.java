package com.crafting.ffxivcraftingaggregator.domain.dto;

import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * A full recipe: what it produces, how many, and everything it consumes.
 *
 * <p>{@code job} is XIVAPI's CraftType ("Smithing"), not the job that performs it ("Blacksmith");
 * clients translate for display. {@code level} may be 0, which means the source data has no
 * level rather than the recipe being level zero.
 *
 * <p>{@code resultQuantity} is what makes costing non-linear - a recipe yielding 3 covers a
 * request for 1, 2 or 3 in a single craft.
 */
@Builder
public record RecipeDto(UUID id,
                        int xivapiId,
                        int resultQuantity,
                        String job,
                        int level,
                        ItemDto resultItem,
                        List<RecipeMaterialsDto> materials) {
}
