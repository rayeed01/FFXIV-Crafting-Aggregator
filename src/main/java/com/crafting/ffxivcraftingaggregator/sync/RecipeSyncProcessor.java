package com.crafting.ffxivcraftingaggregator.sync;

import com.crafting.ffxivcraftingaggregator.client.dto.XivapiRecipeListResponse.RecipeRow;
import com.crafting.ffxivcraftingaggregator.client.dto.XivapiRecipeResponse.ItemRef;
import com.crafting.ffxivcraftingaggregator.client.dto.XivapiRecipeResponse.RecipeFields;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;
import com.crafting.ffxivcraftingaggregator.domain.entity.RecipeMaterials;
import com.crafting.ffxivcraftingaggregator.repository.ItemRepository;
import com.crafting.ffxivcraftingaggregator.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RecipeSyncProcessor {

    private final ItemRepository itemRepository;
    private final RecipeRepository recipeRepository;

    @Transactional
    public void syncRecipe(RecipeRow row){
        RecipeFields f = row.fields();

        Item resultItem = upsertItem(f.itemResult());

        Recipe recipe = recipeRepository.findByXivapiId(row.rowId())
                .orElseGet(Recipe::new);

        recipe.setXivapiId(row.rowId());
        recipe.setResultItem(resultItem);
        recipe.setResultQuantity(f.amountResult());
        recipe.setJob(f.craftType().fields().name());
        recipe.setLevel(f.recipeLevelTable().fields().classJobLevel());

        recipe.getRecipeIngredients().clear();

        List<Integer> amounts = f.amountIngredient();
        List<ItemRef> ingredients = f.ingredients();

        for(int i = 0; i < amounts.size(); i++){
            int amount = amounts.get(i);
            if(amount < 0){
                continue;
            }

            ItemRef ref = ingredients.get(i);
            if(ref.rowId() == null || ref.rowId() <= 0){
                continue;
            }

            Item ingredientItem = upsertItem(ref);
            recipe.getRecipeIngredients().add(RecipeMaterials.builder()
                    .recipe(recipe)
                    .item(ingredientItem)
                    .quantity(amount)
                    .build());
        }
        recipeRepository.save(recipe);
    }

    private Item upsertItem(ItemRef ref){
        String name = ref.fields().name();
        String iconPath;
        if(ref.fields().icon() != null){
            iconPath = ref.fields().icon().path();
        }
        else {
            iconPath = null;
        }

        return itemRepository.findByXivapiId(ref.rowId())
                .map(existing ->{
                    existing.setName(name);
                    existing.setIconUrl(iconPath);
                    return itemRepository.save(existing);
                })
                .orElseGet(() -> itemRepository.save(Item.builder()
                        .xivapiId(ref.rowId())
                        .name(name)
                        .iconUrl(iconPath)
                        .canBeCrafted(false)
                        .build()));
    }
}
