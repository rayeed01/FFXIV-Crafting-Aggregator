package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

/**
 * A data center, for client selectors.
 *
 * <p>The name doubles as a pricing scope: passing it where a world would go prices across every
 * world in the group.
 */
@Builder
public record DataCenterDto(String name,
                            String region) {
}
