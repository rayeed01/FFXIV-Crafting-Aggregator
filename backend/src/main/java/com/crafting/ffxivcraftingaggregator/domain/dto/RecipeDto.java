package com.crafting.ffxivcraftingaggregator.domain.dto;

import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record RecipeDto(UUID id,
                        int xivapiId,
                        int resultQuantity,
                        String job,
                        int level,
                        ItemDto resultItem,
                        List<RecipeMaterialsDto> materials) {
}
