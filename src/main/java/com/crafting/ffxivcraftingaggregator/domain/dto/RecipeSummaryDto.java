package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RecipeSummaryDto(UUID id,
                               int level,
                               String job,
                               String resultItemName,
                               String resultItemIconUrl) {
}
