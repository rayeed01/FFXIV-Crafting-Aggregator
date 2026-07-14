package com.crafting.ffxivcraftingaggregator.mapper.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.mapper.ItemMapper;
import org.springframework.stereotype.Component;

@Component
public class ItemMapperImpl implements ItemMapper {
    @Override
    public ItemDto toDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .xivapiId(item.getXivapiId())
                .name(item.getName())
                .iconUrl(item.getIconUrl())
                .canBeCrafted(item.isCanBeCrafted())
                .build();
    }
}
