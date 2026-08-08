package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sign-in credentials.
 *
 * <p>Sign-in is by username rather than email, matching the JWT subject.
 */
public record LoginRequest(@NotBlank(message = "Username is requires")
                           String username,

                           @NotBlank(message = "Password is requires")
                           String password) {
}
