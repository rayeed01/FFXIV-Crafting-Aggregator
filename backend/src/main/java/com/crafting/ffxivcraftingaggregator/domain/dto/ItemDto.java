package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * An item from the synced catalogue.
 *
 * <p>Two identifiers, both needed: {@code id} is this application's key and appears in its URLs,
 * while {@code xivapiId} is the game's id and is what Universalis and the craft cost endpoint
 * speak in.
 *
 * <p>{@code iconUrl} holds XIVAPI's raw asset path rather than a usable URL, so a client must
 * convert it before rendering. {@code canBeCrafted} is denormalised from "a recipe produces
 * this", set in bulk at the end of a sync.
 */
@Builder
public record ItemDto(UUID id,
                      int xivapiId,
                      String name,
                      String iconUrl,
                      boolean canBeCrafted) {
}
