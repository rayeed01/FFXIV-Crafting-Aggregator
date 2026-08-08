package com.crafting.ffxivcraftingaggregator.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record XivapiRecipeListResponse(@JsonProperty("rows") List<RecipeRow> rows) {

    public record RecipeRow(
            @JsonProperty("row_id") int rowId,
            @JsonProperty("fields") XivapiRecipeResponse.RecipeFields fields){}
}
