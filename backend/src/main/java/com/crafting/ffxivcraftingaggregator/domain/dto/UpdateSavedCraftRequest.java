package com.crafting.ffxivcraftingaggregator.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * A change to a crafting list's title, notes or market.
 *
 * <p>Contents are not touched here; recipes are managed through their own endpoints. Changing the
 * market re-prices nothing on its own - the next cost calculation simply uses the new scope.
 */
@Builder
public record UpdateSavedCraftRequest(@NotBlank(message = "Select a data center")
                                      String dataCenter,

                                      String world,

                                      String notes,

                                      @NotBlank(message = "Title is needed")
                                      String title) {
}
