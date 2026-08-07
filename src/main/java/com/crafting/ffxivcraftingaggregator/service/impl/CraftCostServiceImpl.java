package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.CachedPrice;
import com.crafting.ffxivcraftingaggregator.domain.dto.CraftCostNode;
import com.crafting.ffxivcraftingaggregator.domain.dto.CraftCostNode.Decision;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;
import com.crafting.ffxivcraftingaggregator.domain.entity.RecipeMaterials;
import com.crafting.ffxivcraftingaggregator.exception.ItemNotFoundException;
import com.crafting.ffxivcraftingaggregator.repository.ItemRepository;
import com.crafting.ffxivcraftingaggregator.repository.RecipeRepository;
import com.crafting.ffxivcraftingaggregator.service.CraftCostService;
import com.crafting.ffxivcraftingaggregator.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CraftCostServiceImpl implements CraftCostService {

    private static final Logger log = LoggerFactory.getLogger(CraftCostServiceImpl.class);

    private static final int MAX_DEPTH = 12;

    private final ItemRepository itemRepository;
    private final RecipeRepository recipeRepository;
    private final MarketPriceService marketPriceService;

    @Override
    @Transactional(readOnly = true)
    public CraftCostNode calculate(int itemXivapiId, int quantity, String scope) {
        return calculateAll(Map.of(itemXivapiId, quantity), scope).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CraftCostNode> calculateAll(Map<Integer, Integer> itemQuantities, String scope) {

        if (itemQuantities == null || itemQuantities.isEmpty()) {
            return List.of();
        }

        itemQuantities.forEach((itemId, quantity) -> {
            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1 for item " + itemId);
            }
        });

        Map<Integer, String> rootNames = loadRootNames(itemQuantities.keySet());
        RecipeTree tree = loadTree(itemQuantities.keySet(), rootNames);

        Map<Integer, CachedPrice> prices = marketPriceService.getPrices(List.copyOf(tree.itemNames().keySet()), scope);

        return itemQuantities.entrySet().stream()
                .map(entry -> compute(entry.getKey(), entry.getValue(), tree, prices, new ArrayDeque<>()))
                .toList();
    }

    private Map<Integer, String> loadRootNames(Set<Integer> itemXivapiIds) {
        Map<Integer, String> names = itemRepository.findByXivapiIdIn(itemXivapiIds).stream()
                .collect(Collectors.toMap(Item::getXivapiId, Item::getName));

        Set<Integer> missing = new HashSet<>(itemXivapiIds);
        missing.removeAll(names.keySet());

        if (!missing.isEmpty()) {
            throw new ItemNotFoundException("No item with id " + missing.iterator().next());
        }

        return names;
    }

    private RecipeTree loadTree(Set<Integer> rootItemIds, Map<Integer, String> rootNames) {

        Map<Integer, RecipeNode> recipesByResultItem = new HashMap<>();
        Map<Integer, String> itemNames = new HashMap<>(rootNames);

        Set<Integer> alreadyQueried = new HashSet<>();
        Set<Integer> frontier = new HashSet<>(rootItemIds);

        int depth = 0;

        while (!frontier.isEmpty() && depth < MAX_DEPTH) {

            List<Recipe> recipes = recipeRepository.findByResultItemXivapiIdInWithIngredients(frontier);
            alreadyQueried.addAll(frontier);

            Set<Integer> nextFrontier = new HashSet<>();

            for (Recipe recipe : recipes) {
                int resultItemId = recipe.getResultItem().getXivapiId();

                if (recipesByResultItem.containsKey(resultItemId)) {
                    continue;
                }

                List<Ingredient> ingredients = new ArrayList<>();

                for (RecipeMaterials ri : recipe.getRecipeIngredients()) {
                    int ingredientItemId = ri.getItem().getXivapiId();

                    ingredients.add(new Ingredient(ingredientItemId, ri.getQuantity()));
                    itemNames.put(ingredientItemId, ri.getItem().getName());

                    if (ri.getItem().isCanBeCrafted() && !alreadyQueried.contains(ingredientItemId)) {
                        nextFrontier.add(ingredientItemId);
                    }
                }

                recipesByResultItem.put(resultItemId,
                        new RecipeNode(resultItemId, recipe.getResultQuantity(), ingredients));
            }

            frontier = nextFrontier;
            depth++;
        }

        if (depth >= MAX_DEPTH && !frontier.isEmpty()) {
            log.warn("Recipe tree for items {} hit the depth limit of {}; results may be incomplete",
                    rootItemIds, MAX_DEPTH);
        }

        log.debug("Loaded recipe tree for {} root item(s): {} recipes, {} distinct items, depth {}",
                rootItemIds.size(), recipesByResultItem.size(), itemNames.size(), depth);

        return new RecipeTree(recipesByResultItem, itemNames);
    }

    private CraftCostNode compute(int itemXivapiId,
                                  int quantityNeeded,
                                  RecipeTree tree,
                                  Map<Integer, CachedPrice> prices,
                                  Deque<Integer> path) {

        String itemName = tree.itemNames().getOrDefault(itemXivapiId, "Unknown item");

        if (path.contains(itemXivapiId)) {
            log.warn("Recipe cycle detected at item {} ({})", itemXivapiId, itemName);
            return CraftCostNode.builder()
                    .itemXivapiId(itemXivapiId)
                    .itemName(itemName)
                    .quantityNeeded(quantityNeeded)
                    .decision(Decision.CYCLE)
                    .build();
        }

        CachedPrice price = prices.get(itemXivapiId);

        Long buyCost = null;
        Integer cheapestWorldId = null;

        if (price != null && price.isBuyable()) {
            buyCost = price.minPrice() * quantityNeeded;
            cheapestWorldId = price.cheapestWorldId();
        }

        RecipeNode recipe = tree.recipesByResultItem().get(itemXivapiId);

        Long craftCost = null;
        List<CraftCostNode> children = new ArrayList<>();
        int craftsRequired = 0;
        int surplus = 0;

        if (recipe != null) {
            path.push(itemXivapiId);
            craftsRequired = Math.ceilDiv(quantityNeeded, recipe.resultQuantity());
            surplus = (craftsRequired * recipe.resultQuantity()) - quantityNeeded;

            long ingredientTotal = 0;
            boolean allIngredientsObtainable = true;

            for (Ingredient ingredient : recipe.ingredients()) {
                CraftCostNode child = compute(
                        ingredient.itemXivapiId(),
                        ingredient.quantity() * craftsRequired,
                        tree, prices, path);

                children.add(child);

                if (child.effectiveCost() == null) {
                    allIngredientsObtainable = false;
                } else {
                    ingredientTotal += child.effectiveCost();
                }
            }

            path.pop();

            if (allIngredientsObtainable) {
                craftCost = ingredientTotal;
            }
        }

        Long effectiveCost;
        Decision decision;

        if (buyCost == null && craftCost == null) {
            effectiveCost = null;
            decision = Decision.UNOBTAINABLE;
        } else if (craftCost == null) {
            effectiveCost = buyCost;
            decision = Decision.BUY;
        } else if (buyCost == null || craftCost < buyCost) {
            effectiveCost = craftCost;
            decision = Decision.CRAFT;
        } else {
            effectiveCost = buyCost;
            decision = Decision.BUY;
        }

        return CraftCostNode.builder()
                .itemXivapiId(itemXivapiId)
                .itemName(itemName)
                .quantityNeeded(quantityNeeded)
                .buyCost(buyCost)
                .craftCost(craftCost)
                .effectiveCost(effectiveCost)
                .decision(decision)
                .craftsRequired(craftsRequired)
                .surplus(surplus)
                .cheapestWorldId(cheapestWorldId)
                .ingredients(children)
                .build();
    }

    private record RecipeTree(Map<Integer, RecipeNode> recipesByResultItem,
                              Map<Integer, String> itemNames) {
    }

    private record RecipeNode(int resultItemXivapiId,
                              int resultQuantity,
                              List<Ingredient> ingredients) {
    }

    private record Ingredient(int itemXivapiId, int quantity) {
    }
}