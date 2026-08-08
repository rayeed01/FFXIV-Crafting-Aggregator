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
import com.crafting.ffxivcraftingaggregator.service.MarketPriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
/**
 * Unit tests for the buy-vs-craft algorithm.
 *
 * <p>Everything is mocked - no database, no Redis, no Universalis - because the arithmetic is the
 * only thing under test here. That also means each case can construct exactly the recipe tree and
 * price table it needs, including situations that would be impossible or unreliable to reproduce
 * against live market data (an item nobody is selling, a recipe that loops back on itself).
 *
 * <p>Test fixture, unless a test overrides it:
 * <pre>
 *   Steel Ingot (100)  yield 1   needs 2x Iron Ingot + 1x Coke      buy 500
 *   Iron Ingot  (200)  yield 3   needs 3x Iron Ore + 1x Fire Shard  buy 100
 *   Coke        (201)  yield 1   needs 2x Coal + 2x Fire Shard      buy  80
 *   Iron Ore    (300)  raw                                          buy  20
 *   Coal        (301)  raw                                          buy  30
 *   Fire Shard  (400)  raw                                          buy   5
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CraftCostService")
class CraftCostServiceImplTest {

    private static final String SCOPE = "Faerie";

    private static final int STEEL_INGOT = 100;
    private static final int IRON_INGOT = 200;
    private static final int COKE = 201;
    private static final int IRON_ORE = 300;
    private static final int COAL = 301;
    private static final int FIRE_SHARD = 400;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private MarketPriceService marketPriceService;

    @InjectMocks
    private CraftCostServiceImpl craftCostService;

    private TestWorld world;

    @BeforeEach
    void setUp() {
        world = new TestWorld();

        world.item(STEEL_INGOT, "Steel Ingot", true).price(500);
        world.item(IRON_INGOT, "Iron Ingot", true).price(100);
        world.item(COKE, "Coke", true).price(80);
        world.item(IRON_ORE, "Iron Ore", false).price(20);
        world.item(COAL, "Coal", false).price(30);
        world.item(FIRE_SHARD, "Fire Shard", false).price(5);

        world.recipe(STEEL_INGOT, 1, Map.of(IRON_INGOT, 2, COKE, 1));
        world.recipe(IRON_INGOT, 3, Map.of(IRON_ORE, 3, FIRE_SHARD, 1));
        world.recipe(COKE, 1, Map.of(COAL, 2, FIRE_SHARD, 2));

        world.install();
    }

    // ------------------------------------------------------------------
    // The decision
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("buy versus craft")
    class TheDecision {

        /**
         * Ore collapses in price, so making an Iron Ingot costs more than buying one.
         *
         * <p>Buy is 10 x 3; craft is 3 ore at 20 plus 1 shard at 5.
         */
        @Test
        @DisplayName("buys when the market is cheaper than the ingredients")
        void buysWhenCheaper() {
            world.item(IRON_ORE, "Iron Ore", false).price(20);
            world.item(IRON_INGOT, "Iron Ingot", true).price(10);
            world.install();

            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 3, SCOPE);

            assertThat(result.decision()).isEqualTo(Decision.BUY);
            assertThat(result.buyCost()).isEqualTo(30);
            assertThat(result.craftCost()).isEqualTo(65);
            assertThat(result.effectiveCost()).isEqualTo(30);
        }

        /** Buy is 100 x 3; craft is 65, because a single craft yields all three. */
        @Test
        @DisplayName("crafts when the ingredients are cheaper than the market")
        void craftsWhenCheaper() {
            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 3, SCOPE);

            assertThat(result.decision()).isEqualTo(Decision.CRAFT);
            assertThat(result.buyCost()).isEqualTo(300);
            assertThat(result.craftCost()).isEqualTo(65);
            assertThat(result.effectiveCost()).isEqualTo(65);
        }

        /** Craft cost works out to exactly 65, so the item is priced at 65 to force the tie. */
        @Test
        @DisplayName("a tie goes to buying - same gil, no crafting time, no materials tied up")
        void tieGoesToBuying() {
            world.item(IRON_INGOT, "Iron Ingot", true).price(65);
            world.install();

            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 1, SCOPE);

            assertThat(result.buyCost()).isEqualTo(result.craftCost());
            assertThat(result.decision()).isEqualTo(Decision.BUY);
        }

        @Test
        @DisplayName("an item with no recipe is always BUY, never CRAFT")
        void rawMaterialIsAlwaysBuy() {
            CraftCostNode result = craftCostService.calculate(IRON_ORE, 7, SCOPE);

            assertThat(result.decision()).isEqualTo(Decision.BUY);
            assertThat(result.craftCost()).isNull();
            assertThat(result.buyCost()).isEqualTo(140);
            assertThat(result.ingredients()).isEmpty();
        }

        /**
         * Coke is dumped on the market below the cost of its own ingredients, while Steel Ingot
         * as a whole is still worth crafting. The tree has to reflect both, and the final
         * assertion pins that the parent used Coke's BUY price rather than its craft price.
         */
        @Test
        @DisplayName("each node decides independently - a BUY can sit under a CRAFT")
        void decisionsAreMadePerNode() {
            world.item(COKE, "Coke", true).price(1);
            world.install();

            CraftCostNode steel = craftCostService.calculate(STEEL_INGOT, 1, SCOPE);

            assertThat(steel.decision()).isEqualTo(Decision.CRAFT);

            CraftCostNode coke = childOf(steel, COKE);
            assertThat(coke.decision()).isEqualTo(Decision.BUY);

            CraftCostNode ironIngot = childOf(steel, IRON_INGOT);
            assertThat(steel.craftCost())
                    .isEqualTo(ironIngot.effectiveCost() + coke.effectiveCost());
        }
    }

    // ------------------------------------------------------------------
    // Yield
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("recipe yield")
    class Yield {

        /** The recipe makes 3 and only 1 is wanted: still one craft, and ingredients for all 3. */
        @Test
        @DisplayName("one craft covers a request smaller than the yield, with surplus")
        void yieldCoversSmallerRequest() {
            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 1, SCOPE);

            assertThat(result.craftsRequired()).isEqualTo(1);
            assertThat(result.surplus()).isEqualTo(2);
            assertThat(childOf(result, IRON_ORE).quantityNeeded()).isEqualTo(3);
        }

        @Test
        @DisplayName("rounds crafts UP - 4 wanted from a yield of 3 is two crafts, not one")
        void roundsCraftsUp() {
            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 4, SCOPE);

            assertThat(result.craftsRequired()).isEqualTo(2);
            assertThat(result.surplus()).isEqualTo(2);
        }

        /**
         * The subtle one. Wanting 4 of a yield-3 recipe means 2 crafts, so 3 x 2 = 6 ore. Scaling
         * by the requested 4 would charge for 12 ore, three times too much.
         *
         * <p>Craft cost is 6 ore at 20 plus 2 shards at 5.
         */
        @Test
        @DisplayName("ingredients scale by CRAFTS, not by the quantity requested")
        void ingredientsScaleByCrafts() {
            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 4, SCOPE);

            assertThat(childOf(result, IRON_ORE).quantityNeeded()).isEqualTo(6);
            assertThat(childOf(result, FIRE_SHARD).quantityNeeded()).isEqualTo(2);
            assertThat(result.craftCost()).isEqualTo(130);
        }

        @Test
        @DisplayName("an exact multiple of the yield leaves no surplus")
        void exactMultipleHasNoSurplus() {
            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 6, SCOPE);

            assertThat(result.craftsRequired()).isEqualTo(2);
            assertThat(result.surplus()).isZero();
        }
    }

    // ------------------------------------------------------------------
    // Unobtainable items
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("items that cannot be obtained")
    class Unobtainable {

        /**
         * A zero cost here would make the item look free and every craft using it look
         * profitable, so the absence of a price has to stay null.
         */
        @Test
        @DisplayName("an unlisted raw material is UNOBTAINABLE with a null cost, never zero")
        void unlistedRawMaterialIsUnobtainable() {
            world.unlisted(IRON_ORE);
            world.install();

            CraftCostNode result = craftCostService.calculate(IRON_ORE, 5, SCOPE);

            assertThat(result.decision()).isEqualTo(Decision.UNOBTAINABLE);
            assertThat(result.effectiveCost()).isNull();
            assertThat(result.buyCost()).isNull();
        }

        /**
         * Summing only the obtainable ingredients would report 60 and look like a bargain against
         * the buy price of 300 - for a craft that cannot actually be completed.
         */
        @Test
        @DisplayName("one unobtainable ingredient makes the whole craft cost unknown")
        void oneBadIngredientNullsTheCraftCost() {
            world.unresolved(FIRE_SHARD);
            world.install();

            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 3, SCOPE);

            assertThat(result.craftCost()).isNull();
            assertThat(result.decision()).isEqualTo(Decision.BUY);
            assertThat(result.effectiveCost()).isEqualTo(300);
        }

        @Test
        @DisplayName("an item that can be neither bought nor crafted is UNOBTAINABLE")
        void neitherBuyableNorCraftableIsUnobtainable() {
            world.unresolved(FIRE_SHARD);
            world.unlisted(IRON_INGOT);
            world.install();

            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 1, SCOPE);

            assertThat(result.decision()).isEqualTo(Decision.UNOBTAINABLE);
            assertThat(result.effectiveCost()).isNull();
        }

        /**
         * Coal cannot be had, so Coke cannot be crafted; Coke also cannot be bought, so Steel
         * Ingot cannot be crafted either. Only its own market price remains.
         */
        @Test
        @DisplayName("unobtainability propagates all the way up the tree")
        void unobtainabilityPropagatesUpwards() {
            world.unresolved(COAL);
            world.unlisted(COKE);
            world.install();

            CraftCostNode steel = craftCostService.calculate(STEEL_INGOT, 1, SCOPE);

            assertThat(childOf(steel, COKE).decision()).isEqualTo(Decision.UNOBTAINABLE);
            assertThat(steel.craftCost()).isNull();
            assertThat(steel.decision()).isEqualTo(Decision.BUY);
            assertThat(steel.effectiveCost()).isEqualTo(500);
        }

        @Test
        @DisplayName("an unbuyable item that CAN be crafted is still obtainable")
        void unbuyableButCraftableIsCraft() {
            world.unlisted(IRON_INGOT);
            world.install();

            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 3, SCOPE);

            assertThat(result.buyCost()).isNull();
            assertThat(result.decision()).isEqualTo(Decision.CRAFT);
            assertThat(result.effectiveCost()).isEqualTo(65);
        }
    }

    // ------------------------------------------------------------------
    // Tree traversal
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("tree traversal")
    class Traversal {

        /**
         * Fire Shard sits under both Iron Ingot and Coke. A naive "already visited" guard would
         * skip the second and silently understate Coke's cost.
         *
         * <p>The quantities differ because each parent needs its own amount.
         */
        @Test
        @DisplayName("a shared ingredient is costed separately in each branch that uses it")
        void sharedIngredientIsCostedInBothBranches() {
            CraftCostNode steel = craftCostService.calculate(STEEL_INGOT, 1, SCOPE);

            CraftCostNode underIron = childOf(childOf(steel, IRON_INGOT), FIRE_SHARD);
            CraftCostNode underCoke = childOf(childOf(steel, COKE), FIRE_SHARD);

            assertThat(underIron).isNotNull();
            assertThat(underCoke).isNotNull();
            assertThat(underIron.quantityNeeded()).isNotEqualTo(underCoke.quantityNeeded());
        }

        /**
         * Not something FFXIV data should contain, but recursion over 14.9k rows of third-party
         * data must not be left unbounded.
         */
        @Test
        @DisplayName("a recipe that requires itself is reported as a CYCLE, not a stack overflow")
        void selfReferencingRecipeIsACycle() {
            world.recipe(IRON_INGOT, 1, Map.of(IRON_INGOT, 1));
            world.install();

            CraftCostNode result = craftCostService.calculate(IRON_INGOT, 1, SCOPE);

            assertThat(result.ingredients()).anySatisfy(child ->
                    assertThat(child.decision()).isEqualTo(Decision.CYCLE));
        }

        @Test
        @DisplayName("costs are for the quantity needed, not per unit, so parents can sum directly")
        void costsAreTotalsNotUnitPrices() {
            CraftCostNode result = craftCostService.calculate(IRON_ORE, 5, SCOPE);

            assertThat(result.quantityNeeded()).isEqualTo(5);
            assertThat(result.buyCost()).isEqualTo(100);
        }

        @Test
        @DisplayName("item names are carried through to every node")
        void namesArePopulated() {
            CraftCostNode steel = craftCostService.calculate(STEEL_INGOT, 1, SCOPE);

            assertThat(steel.itemName()).isEqualTo("Steel Ingot");
            assertThat(childOf(steel, COKE).itemName()).isEqualTo("Coke");
            assertThat(childOf(childOf(steel, COKE), COAL).itemName()).isEqualTo("Coal");
        }
    }

    // ------------------------------------------------------------------
    // Batching
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("calculateAll")
    class Batch {

        /**
         * The whole point of the batch path. Two separate calculate() calls would be two lookups,
         * and a crafting list of ten recipes would be ten.
         */
        @Test
        @DisplayName("prices every requested item in ONE market lookup")
        void oneMarketLookupForTheWholeBatch() {
            craftCostService.calculateAll(Map.of(STEEL_INGOT, 1, COKE, 2), SCOPE);

            verify(marketPriceService, times(1)).getPrices(any(), anyString());
        }

        @Test
        @DisplayName("returns one tree per requested item")
        void returnsOneTreePerItem() {
            List<CraftCostNode> results =
                    craftCostService.calculateAll(Map.of(STEEL_INGOT, 1, COKE, 2), SCOPE);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(CraftCostNode::itemXivapiId)
                    .containsExactlyInAnyOrder(STEEL_INGOT, COKE);
        }

        /**
         * Coke is both a root in its own right and an ingredient of Steel Ingot. Once its recipe
         * is loaded for one, the other must not trigger a second query.
         */
        @Test
        @DisplayName("a shared sub-recipe is not queried twice")
        void sharedSubtreeIsLoadedOnce() {
            craftCostService.calculateAll(Map.of(STEEL_INGOT, 1, COKE, 2), SCOPE);

            List<Collection<Integer>> queried = world.queriedBatches();

            long timesCokeWasQueried = queried.stream()
                    .filter(batch -> batch.contains(COKE))
                    .count();

            assertThat(timesCokeWasQueried).isEqualTo(1);
        }

        @Test
        @DisplayName("an empty request does no work at all")
        void emptyRequestDoesNothing() {
            assertThat(craftCostService.calculateAll(Map.of(), SCOPE)).isEmpty();

            verifyNoInteractions(marketPriceService);
            verifyNoInteractions(recipeRepository);
        }
    }

    // ------------------------------------------------------------------
    // Input guards
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("input validation")
    class Guards {

        /** A zero quantity would cost 0 gil and read downstream as free. */
        @Test
        @DisplayName("rejects a quantity below 1 before doing any work")
        void rejectsZeroQuantity() {
            assertThatThrownBy(() -> craftCostService.calculate(IRON_ORE, 0, SCOPE))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(marketPriceService);
        }

        @Test
        @DisplayName("rejects an unknown item rather than returning an empty tree")
        void rejectsUnknownItem() {
            assertThatThrownBy(() -> craftCostService.calculate(999999, 1, SCOPE))
                    .isInstanceOf(ItemNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Finds a direct child by item id, failing the test rather than returning null. */
    private static CraftCostNode childOf(CraftCostNode parent, int itemXivapiId) {
        return parent.ingredients().stream()
                .filter(child -> child.itemXivapiId() == itemXivapiId)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No child with item id %d under %s".formatted(itemXivapiId, parent.itemName())));
    }

    /**
     * Builds the item, recipe and price fixtures and wires them into the mocks.
     *
     * <p>Exists so each test can describe the world it needs in a few lines instead of stubbing
     * three repositories by hand. Call install() after any change.
     */
    private class TestWorld {

        private final Map<Integer, Item> items = new HashMap<>();
        private final Map<Integer, Recipe> recipes = new HashMap<>();
        private final Map<Integer, CachedPrice> prices = new HashMap<>();
        private final List<Collection<Integer>> queriedBatches = new ArrayList<>();

        ItemBuilder item(int xivapiId, String name, boolean craftable) {
            Item item = new Item();
            item.setId(UUID.randomUUID());
            item.setXivapiId(xivapiId);
            item.setName(name);
            item.setCanBeCrafted(craftable);
            items.put(xivapiId, item);
            return new ItemBuilder(xivapiId);
        }

        void recipe(int resultItemId, int resultQuantity, Map<Integer, Integer> ingredients) {
            Recipe recipe = new Recipe();
            recipe.setId(UUID.randomUUID());
            recipe.setXivapiId(resultItemId);
            recipe.setResultItem(items.get(resultItemId));
            recipe.setResultQuantity(resultQuantity);

            List<RecipeMaterials> materials = new ArrayList<>();
            ingredients.forEach((ingredientId, quantity) -> {
                RecipeMaterials material = new RecipeMaterials();
                material.setId(UUID.randomUUID());
                material.setRecipe(recipe);
                material.setItem(items.get(ingredientId));
                material.setQuantity(quantity);
                materials.add(material);
            });

            recipe.setRecipeIngredients(materials);
            recipes.put(resultItemId, recipe);
        }

        void unlisted(int xivapiId) {
            prices.put(xivapiId, CachedPrice.unlisted());
        }

        void unresolved(int xivapiId) {
            prices.put(xivapiId, CachedPrice.unresolved());
        }

        List<Collection<Integer>> queriedBatches() {
            return queriedBatches;
        }

        /** Re-stubs the mocks from the current fixture state. */
        void install() {
            doAnswer(invocation -> {
                Collection<Integer> ids = invocation.getArgument(0);
                return ids.stream().filter(items::containsKey).map(items::get).toList();
            }).when(itemRepository).findByXivapiIdIn(any());

            doAnswer(invocation -> {
                Collection<Integer> ids = invocation.getArgument(0);
                queriedBatches.add(List.copyOf(ids));
                return ids.stream().filter(recipes::containsKey).map(recipes::get).toList();
            }).when(recipeRepository).findByResultItemXivapiIdInWithIngredients(any());

            doAnswer(invocation -> {
                List<Integer> ids = invocation.getArgument(0);
                Map<Integer, CachedPrice> result = new HashMap<>();
                ids.forEach(id -> result.put(id, prices.get(id)));
                return result;
            }).when(marketPriceService).getPrices(any(), anyString());
        }

        private class ItemBuilder {
            private final int xivapiId;

            ItemBuilder(int xivapiId) {
                this.xivapiId = xivapiId;
            }

            /** Listed at NQ only, with no world attribution - the shape most of these tests need. */
            void price(long minPrice) {
                prices.put(xivapiId, new CachedPrice(
                        CachedPrice.Status.PRICED, minPrice, minPrice, null, null, null, null));
            }

            /** Listed at both qualities, for the HQ/NQ selection tests. */
            void price(long nqPrice, long hqPrice) {
                prices.put(xivapiId, new CachedPrice(
                        CachedPrice.Status.PRICED,
                        Math.min(nqPrice, hqPrice),
                        nqPrice,
                        hqPrice,
                        null, null, null));
            }
        }
    }
}