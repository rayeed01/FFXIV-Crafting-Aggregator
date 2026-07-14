package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;

public interface ItemMapper {
    ItemDto toDto(Item item);
}
