package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(@NotBlank(message = "Default data center is required")
                                String defaultDataCenter,

                                @NotBlank(message = "Default world is required")
                                String defaultWorld) {
}
