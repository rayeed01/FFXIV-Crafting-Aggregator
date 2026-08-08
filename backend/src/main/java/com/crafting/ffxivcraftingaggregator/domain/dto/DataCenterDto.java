package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

@Builder
public record DataCenterDto(String name,
                            String region) {
}
