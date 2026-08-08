package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

/**
 * A world, flattened for client selectors.
 *
 * <p>Carries its data center and region inline rather than nesting them, so a client can group a
 * flat list without a second lookup.
 *
 * <p>{@code universalisId} is what price results report as their source world, and is how a
 * "cheapest on..." hint is resolved back to a name.
 */
@Builder
public record WorldDto(String name,
                       int universalisId,
                       String dataCenter,
                       String region) {
}
