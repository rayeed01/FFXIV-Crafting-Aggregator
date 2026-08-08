package com.crafting.ffxivcraftingaggregator.mapper.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeMaterialsDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.RecipeMaterials;
import com.crafting.ffxivcraftingaggregator.mapper.ItemMapper;
import com.crafting.ffxivcraftingaggregator.mapper.RecipeMaterialsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Straight field copy, delegating the item to its own mapper.
 */
@Component
@RequiredArgsConstructor
public class RecipeMaterialsMapperImpl implements RecipeMaterialsMapper {

    private final ItemMapper itemMapper;

    @Override
    public RecipeMaterialsDto toDto(RecipeMaterials recipeMaterials) {
        return RecipeMaterialsDto.builder()
                .id(recipeMaterials.getId())
                .item(itemMapper.toDto(recipeMaterials.getItem()))
                .quantity(recipeMaterials.getQuantity())
                .build();
    }
}
