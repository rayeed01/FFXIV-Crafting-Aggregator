package com.crafting.ffxivcraftingaggregator.client.dto;

import java.util.List;

/**
 * A data center as Universalis reports it, including the ids of its worlds.
 *
 * <p>This is the only place the world-to-data-center relationship appears: the world endpoint does
 * not say which data center a world belongs to, so the sync derives it from here.
 */
public record UniversalisDataCenter(String name, String region, List<Integer> worlds) {
}
