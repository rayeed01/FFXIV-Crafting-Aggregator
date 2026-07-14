package com.crafting.ffxivcraftingaggregator.mapper.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.RecipeDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.SavedCraftDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.SavedCraftSummaryDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.SavedCraft;
import com.crafting.ffxivcraftingaggregator.mapper.RecipeMapper;
import com.crafting.ffxivcraftingaggregator.mapper.SavedCraftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SavedCraftMapperImpl implements SavedCraftMapper {

    private final RecipeMapper recipeMapper;

    @Override
    public SavedCraftDto toDto(SavedCraft savedCraft) {
        List<RecipeDto> recipes = savedCraft.getSavedCraftRecipes().stream()
                .map(savedCraftRecipes -> recipeMapper.toDto(savedCraftRecipes.getRecipe()))
                .toList();

        return SavedCraftDto.builder()
                .id(savedCraft.getId())
                .title(savedCraft.getTitle())
                .dataCenter(savedCraft.getDataCenter())
                .world(savedCraft.getWorld())
                .notes(savedCraft.getNotes())
                .createdAt(savedCraft.getCreatedAt())
                .updatedAt(savedCraft.getUpdatedAt())
                .recipes(recipes)
                .build();
    }

    @Override
    public SavedCraftSummaryDto toSummaryDto(SavedCraft savedCraft) {
        return SavedCraftSummaryDto.builder()
                .id(savedCraft.getId())
                .title(savedCraft.getTitle())
                .dataCenter(savedCraft.getDataCenter())
                .world(savedCraft.getWorld())
                .notes(savedCraft.getNotes())
                .recipeCount(savedCraft.getSavedCraftRecipes().size())
                .createdAt(savedCraft.getCreatedAt())
                .updatedAt(savedCraft.getUpdatedAt())
                .build();
    }
}
