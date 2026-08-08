package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

@Builder
public record WorldDto(String name,
                       int universalisId,
                       String dataCenter,
                       String region) {
}
