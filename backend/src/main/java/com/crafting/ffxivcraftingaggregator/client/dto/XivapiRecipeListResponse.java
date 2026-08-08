package com.crafting.ffxivcraftingaggregator.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw response shape of an XIVAPI recipe page.
 *
 * <p>Paging is by cursor rather than offset, so the sync walks pages until one comes back short.
 */
public record XivapiRecipeListResponse(@JsonProperty("rows") List<RecipeRow> rows) {

    public record RecipeRow(
            @JsonProperty("row_id") int rowId,
            @JsonProperty("fields") XivapiRecipeResponse.RecipeFields fields){}
}
