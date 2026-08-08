package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CreateSavedCraftRequest(@NotBlank(message = "Select a data center")
                                   String dataCenter,

                                   String world,

                                   String notes,

                                   @NotBlank(message = "Title is needed")
                                   String title,

                                   @NotNull(message = "Recipe list cant be null")
                                   List<@Valid @NotNull SavedCraftRecipeRequest> recipes){
}
