package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RecipeMaterialsDto(UUID id,
                                 ItemDto item,
                                 int quantity) {
}
