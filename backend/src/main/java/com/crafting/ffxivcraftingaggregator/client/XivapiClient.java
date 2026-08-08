package com.crafting.ffxivcraftingaggregator.client;

import com.crafting.ffxivcraftingaggregator.client.dto.XivapiRecipeListResponse;

import java.util.List;

public interface XivapiClient {
    List<XivapiRecipeListResponse.RecipeRow> listRecipes(int limit, Integer after);
}
