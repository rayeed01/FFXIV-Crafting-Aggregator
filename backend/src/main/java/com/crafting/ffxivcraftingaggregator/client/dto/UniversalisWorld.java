package com.crafting.ffxivcraftingaggregator.client.dto;

/**
 * A world as Universalis reports it: an id and a name, with no data center.
 *
 * <p>The relationship is only available from the data center endpoint, so the sync reads both.
 */
public record UniversalisWorld(int id, String name) {
}
