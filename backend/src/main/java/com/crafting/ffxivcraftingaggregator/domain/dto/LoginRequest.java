package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "Username is requires")
                           String username,

                           @NotBlank(message = "Password is requires")
                           String password) {
}
