package com.crafting.ffxivcraftingaggregator.service;



import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

/**
 * Read access to the item catalogue imported from XIVAPI.
 *
 * <p>Items are identified two ways. {@link UUID} is this application's own key and is what the
 * REST API exposes; {@code xivapiId} is the game's id, and is what Universalis and the craft cost
 * endpoint speak in. Both are needed, and neither can replace the other.
 */
public interface ItemService {

    /**
     * @param id this application's item id, not the XIVAPI one
     * @throws com.crafting.ffxivcraftingaggregator.exception.ItemNotFoundException if no such item
     */
    ItemDto findItemById(UUID id);

    /**
     * Case-insensitive substring match on item name.
     *
     * <p>Results are capped, because an unbounded substring search over ~14k items returns
     * thousands of rows for a single letter. A blank query yields an empty list rather than
     * everything.
     */
    List<ItemDto> searchItems(String query);

    /**
     * Looks an item up by its XIVAPI id, for internal callers that already speak that id.
     *
     * <p>Returns the entity rather than a DTO, and an empty Optional rather than throwing, because
     * the callers are services deciding what to do next rather than endpoints returning a response.
     */
    Optional<Item> getItemByXivapiId(int xivapiId);

}
