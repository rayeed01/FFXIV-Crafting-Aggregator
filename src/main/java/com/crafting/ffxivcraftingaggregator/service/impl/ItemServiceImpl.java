package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.exception.ItemNotFoundException;
import com.crafting.ffxivcraftingaggregator.mapper.ItemMapper;
import com.crafting.ffxivcraftingaggregator.repository.ItemRepository;
import com.crafting.ffxivcraftingaggregator.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Transactional(readOnly = true)
    @Override
    public ItemDto findItemById(UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found"));

        return itemMapper.toDto(item);
    }

    @Transactional
    @Override
    public List<ItemDto> searchItems(String query) {
        return itemRepository.findByNameContainingIgnoreCase(query).stream()
                .map(itemMapper::toDto)
                .toList();
    }

    @Override
    public Optional<Item> getItemByXivapiId(int xivapiId) {
        return itemRepository.findByXivapiId(xivapiId);
    }
}
