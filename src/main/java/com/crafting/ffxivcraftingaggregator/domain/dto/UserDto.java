package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserDto(UUID id,
                      String email,
                      String username,
                      String defaultDataCenter,
                      String defaultWorld) {
}
