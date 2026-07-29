package com.crafting.ffxivcraftingaggregator.service;



import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemService {
    ItemDto findItemById(UUID id);
    List<ItemDto> searchItems(String query);
    Optional<Item> getItemByXivapiId(int xivapiId);

}
