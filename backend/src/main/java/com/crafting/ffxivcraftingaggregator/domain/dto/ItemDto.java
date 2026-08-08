package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ItemDto(UUID id,
                      int xivapiId,
                      String name,
                      String iconUrl,
                      boolean canBeCrafted) {
}
