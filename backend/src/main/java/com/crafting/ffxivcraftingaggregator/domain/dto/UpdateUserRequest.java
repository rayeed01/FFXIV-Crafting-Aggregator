package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A change to the user's default market.
 *
 * <p>Both fields are required, so this replaces the pair rather than patching one half and
 * risking an inconsistent combination.
 */
public record UpdateUserRequest(@NotBlank(message = "Default data center is required")
                                String defaultDataCenter,

                                @NotBlank(message = "Default world is required")
                                String defaultWorld) {
}
