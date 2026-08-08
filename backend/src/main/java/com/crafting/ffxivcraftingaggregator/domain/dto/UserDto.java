package com.crafting.ffxivcraftingaggregator.domain.dto;

import com.crafting.ffxivcraftingaggregator.domain.entity.Role;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserDto(UUID id,
                      String email,
                      String username,
                      String defaultDataCenter,
                      String defaultWorld,
                      Role role,
                      LocalDateTime createdAt) {
}
