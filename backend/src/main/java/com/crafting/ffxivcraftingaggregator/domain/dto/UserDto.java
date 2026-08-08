package com.crafting.ffxivcraftingaggregator.domain.dto;

import com.crafting.ffxivcraftingaggregator.domain.entity.Role;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user profile as returned to that user.
 *
 * <p>Deliberately carries no password hash of any kind. {@code role} is included so a client can
 * hide navigation it cannot use, which is presentation only - the server enforces roles
 * regardless of what the client shows.
 */
@Builder
public record UserDto(UUID id,
                      String email,
                      String username,
                      String defaultDataCenter,
                      String defaultWorld,
                      Role role,
                      LocalDateTime createdAt) {
}
