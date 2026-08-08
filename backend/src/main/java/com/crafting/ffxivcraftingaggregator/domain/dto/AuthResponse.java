package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

@Builder
public record AuthResponse(String token) {
}
