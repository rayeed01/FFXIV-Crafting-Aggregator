package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;

/**
 * Converts an {@link Item} entity into the shape the API returns.
 */
public interface ItemMapper {
    ItemDto toDto(Item item);
}
