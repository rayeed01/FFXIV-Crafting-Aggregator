package com.crafting.ffxivcraftingaggregator.client.impl;

import com.crafting.ffxivcraftingaggregator.client.XivapiClient;
import com.crafting.ffxivcraftingaggregator.client.dto.XivapiRecipeListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * XIVAPI client for the item and recipe catalogue.
 *
 * <p>Requests only the fields the sync actually stores rather than whole rows, since a recipe row
 * upstream carries a great deal this application has no use for.
 *
 * <p>Used exclusively by the background sync. Nothing in the request path calls XIVAPI - once
 * imported, recipes are served from the local database.
 */
@Component
public class XivapiClientImpl implements XivapiClient {

    private static final String RECIPE_FIELDS = String.join(",",
            "AmountResult",
            "AmountIngredient",
            "CraftType.Name",
            "ItemResult.Name",
            "ItemResult.Icon",
            "Ingredient[].Name",
            "Ingredient[].Icon",
            "RecipeLevelTable.ClassJobLevel"
    );

    private final RestClient restClient;

    public XivapiClientImpl(RestClient.Builder builder,
                            @Value("${xivapi.base-url}") String baseUrl){
        this.restClient = builder.baseUrl(baseUrl).build();
    }


    @Override
    public List<XivapiRecipeListResponse.RecipeRow> listRecipes(int limit, Integer after) {
        XivapiRecipeListResponse response = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .path("api/sheet/Recipe")
                            .queryParam("limit", limit)
                            .queryParam("fields", RECIPE_FIELDS);
                    if(after != null){
                        uriBuilder.queryParam("after", after);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(XivapiRecipeListResponse.class);

        return response == null ? List.of() : response.rows();
    }
}
