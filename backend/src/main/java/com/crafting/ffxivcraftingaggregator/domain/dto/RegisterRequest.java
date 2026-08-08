package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank(message = "Username is required")
                              @Size(min = 3, max = 20, message = "Username must be between {min} and {max} characters")
                              @Pattern(regexp = "^[\\w\\s]+$", message = "Username can only have letters, numbers, and spaces")
                              String username,

                              @NotBlank(message = "Email is required")
                              @Email(message = "Must be a valid email")
                              String email,

                              @NotBlank
                              @Size(min = 8, message = "Password must be at least {min} characters")
                              String password,

                              @NotBlank(message = "Choose a data center")
                              String defaultDataCenter,

                              @NotBlank(message = "Choose a world")
                              String defaultWorld) {
}
