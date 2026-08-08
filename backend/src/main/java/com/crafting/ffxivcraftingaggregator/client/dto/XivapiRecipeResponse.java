package com.crafting.ffxivcraftingaggregator.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw response shape of a single XIVAPI recipe row.
 *
 * <p>The nesting mirrors XIVAPI's: fields wrapped in a {@code fields} object, and references
 * expanded into their own row objects. Ingredients arrive as two parallel lists - the items and
 * their amounts - which the sync zips together.
 */
public record XivapiRecipeResponse(@JsonProperty("row_id") int rowId,
                                   @JsonProperty("fields") RecipeFields fields) {

    public record RecipeFields(@JsonProperty("AmountResult") int amountResult,
                               @JsonProperty("AmountIngredient")List<Integer> amountIngredient,
                               @JsonProperty("CraftType") CraftTypeRef craftType,
                               @JsonProperty("Ingredient") List<ItemRef> ingredients,
                               @JsonProperty("ItemResult") ItemRef itemResult,
                               @JsonProperty("RecipeLevelTable") RecipeLevelRef recipeLevelTable){}

    public record CraftTypeRef(@JsonProperty("row_id") Integer rowId,
                               @JsonProperty("fields") CraftTypeFields fields){}

    public record CraftTypeFields(
            @JsonProperty("Name") String name){}

    public record ItemRef(@JsonProperty("row_id") Integer rowId,
                          @JsonProperty("fields") ItemFields fields){}

    public record ItemFields(@JsonProperty("Name") String name,
                             @JsonProperty("Icon") IconStruct icon){}

    public record IconStruct(@JsonProperty("id") int id,
                             @JsonProperty("path") String path){}

    public record RecipeLevelRef(
            @JsonProperty("fields") RecipeLevelFields fields){}

    public record RecipeLevelFields(
            @JsonProperty("ClassJobLevel") int classJobLevel){}
}
