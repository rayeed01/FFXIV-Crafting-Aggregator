package com.crafting.ffxivcraftingaggregator.client;

import com.crafting.ffxivcraftingaggregator.client.dto.XivapiRecipeListResponse;

import java.util.List;

/**
 * Outbound calls to XIVAPI for the item and recipe catalogue.
 *
 * <p>Used only by the sync. Nothing in the request path talks to XIVAPI - recipes are read from
 * the local database once imported.
 */
public interface XivapiClient {
    List<XivapiRecipeListResponse.RecipeRow> listRecipes(int limit, Integer after);
}
