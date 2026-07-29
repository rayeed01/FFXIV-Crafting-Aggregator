package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateSavedCraftRequest(@NotBlank(message = "Select a data center")
                                      String dataCenter,

                                      @NotBlank(message = "Select a world")
                                      String world,

                                      String notes,

                                      @NotBlank(message = "Title is needed")
                                      String title) {
}
