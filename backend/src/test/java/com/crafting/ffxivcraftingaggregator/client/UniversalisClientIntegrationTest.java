package com.crafting.ffxivcraftingaggregator.client;

import com.crafting.ffxivcraftingaggregator.client.dto.ItemPrice;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisDataCenter;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisWorld;
import com.crafting.ffxivcraftingaggregator.client.impl.UniversalisClientImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hits the real Universalis aggregated endpoint. Excluded from `mvn test`;
 * run with `mvn test -Pintegration`.
 *
 * <p>No Spring context and no database - the client is constructed directly, so this cannot touch
 * the seeded Postgres data.
 *
 * <p>Assertions are on SHAPE, never on specific prices. Market data changes between runs, so
 * asserting an exact gil value would produce a test that passes today and fails tomorrow.
 */
@Tag("integration")
@DisplayName("UniversalisClient against the live aggregated endpoint")
class UniversalisClientIntegrationTest {

    /** Base URL only - the impl appends /api/v2/... itself. Must match universalis.base-url. */
    private static final String BASE_URL = "https://universalis.app";

    private static final String DATA_CENTER = "Aether";
    private static final String WORLD = "Faerie";

    /** Commonly-listed cheap crafting mats; should have listings on any populated data centre. */
    private static final int KNOWN_ITEM = 5057;
    private static final int SECOND_ITEM = 5056;

    /** Gil. Exists in-game, never appears on the market board. */
    private static final int UNTRADEABLE_ITEM = 1;

    /**
     * Declared locally rather than read from the impl. A test asserting against the same constant
     * the production code uses would still pass if that constant were wrong.
     */
    private static final int UNIVERSALIS_CAP = 100;

    private static UniversalisClient client;

    @BeforeAll
    static void setUp() {
        client = new UniversalisClientImpl(RestClient.builder(), BASE_URL);
    }

    // ---------------------------------------------------------------------
    // Input guards - no network involved
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("empty id list short-circuits without touching the network")
    void emptyListReturnsEmptyResult() {
        UniversalisPrices result = client.getPrices(List.of(), DATA_CENTER);

        assertThat(result.prices()).isEmpty();
        assertThat(result.unresolved()).isEmpty();
    }

    @Test
    @DisplayName("null id list is tolerated rather than throwing")
    void nullListReturnsEmptyResult() {
        UniversalisPrices result = client.getPrices(null, DATA_CENTER);

        assertThat(result.prices()).isEmpty();
        assertThat(result.unresolved()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Core behaviour
    // ---------------------------------------------------------------------

    /**
     * The item id assertion is not redundant with the map key: the key is built from the response
     * payload rather than the request, so if the itemId field ever stopped binding it would
     * deserialise to 0 and every lookup would silently mis-key.
     */
    @Test
    @DisplayName("returns usable price data for a commonly-listed item")
    void returnsPriceDataForKnownItem() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM), DATA_CENTER);

        assertThat(result.unresolved()).isEmpty();
        assertThat(result.prices()).containsKey(KNOWN_ITEM);

        ItemPrice price = result.prices().get(KNOWN_ITEM);

        assertThat(price.itemXivapiId()).isEqualTo(KNOWN_ITEM);
        assertThat(price.hasListing()).isTrue();
        assertThat(price.minPrice()).isNotNull().isPositive();
    }

    /**
     * Unlike the old current-data endpoint, the aggregated endpoint returns the same wrapper shape
     * for one id as for many - which is why the client no longer needs a single-item branch.
     * This pins that, since a regression would reintroduce the branch divergence.
     */
    /**
     * Prices may legitimately differ between the two calls if a listing sold in between, so only
     * the structurally stable fields are compared.
     */
    @Test
    @DisplayName("one id and many ids take the same code path")
    void singleAndMultiRequestsBehaveIdentically() {
        ItemPrice alone = client.getPrices(List.of(KNOWN_ITEM), DATA_CENTER)
                .prices().get(KNOWN_ITEM);
        ItemPrice together = client.getPrices(List.of(KNOWN_ITEM, SECOND_ITEM), DATA_CENTER)
                .prices().get(KNOWN_ITEM);

        assertThat(together.itemXivapiId()).isEqualTo(alone.itemXivapiId());
        assertThat(together.hasListing()).isEqualTo(alone.hasListing());
    }

    @Test
    @DisplayName("multiple ids each return their own entry")
    void multipleItemsEachReturnAnEntry() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM, SECOND_ITEM), DATA_CENTER);

        assertThat(result.prices()).containsOnlyKeys(KNOWN_ITEM, SECOND_ITEM);
        assertThat(result.unresolved()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // The reason for switching endpoints
    // ---------------------------------------------------------------------

    /**
     * The whole point of the aggregated endpoint: a data centre wide query reports WHICH world
     * holds the cheapest listing. Without it the user is told "50 gil somewhere on Aether" and has
     * no idea where to travel.
     */
    @Test
    @DisplayName("a data centre query identifies which world holds the cheapest listing")
    void dataCentreQueryReportsCheapestWorld() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM), DATA_CENTER);

        ItemPrice price = result.prices().get(KNOWN_ITEM);

        assertThat(price.cheapestWorldId())
                .as("cheapest world must be identified for a DC-scoped query")
                .isNotNull()
                .isPositive();
    }

    /**
     * minPrice is computed by the client, not returned by the API - the aggregated endpoint
     * reports NQ and HQ separately with no combined figure. HQ regularly undercuts NQ on cheap
     * materials, and a crafter buying an ingredient does not care about quality, so taking NQ
     * blindly would overstate craft costs.
     */
    @Test
    @DisplayName("minPrice is the cheaper of the NQ and HQ listings")
    /**
     * minPrice must not merely be less than or equal to both qualities - it has to actually BE
     * one of them, rather than some third number arrived at by averaging or rounding.
     */
    void minPriceIsTheCheaperQuality() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM, SECOND_ITEM), DATA_CENTER);

        assertThat(result.prices().values()).allSatisfy(price -> {
            if (price.minPriceNq() != null) {
                assertThat(price.minPrice()).isLessThanOrEqualTo(price.minPriceNq());
            }
            if (price.minPriceHq() != null) {
                assertThat(price.minPrice()).isLessThanOrEqualTo(price.minPriceHq());
            }
            assertThat(price.minPrice())
                    .isIn(price.minPriceNq(), price.minPriceHq());
        });
    }

    @Test
    @DisplayName("a world-scoped query also returns usable data")
    void worldScopedQueryWorks() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM), WORLD);

        assertThat(result.prices()).containsKey(KNOWN_ITEM);
        assertThat(result.prices().get(KNOWN_ITEM).minPrice()).isNotNull().isPositive();
    }

    // ---------------------------------------------------------------------
    // Unresolved items
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("untradeable item is reported as unresolved, not merely absent")
    void untradeableItemIsUnresolved() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM, UNTRADEABLE_ITEM), DATA_CENTER);

        assertThat(result.prices()).containsKey(KNOWN_ITEM);
        assertThat(result.prices()).doesNotContainKey(UNTRADEABLE_ITEM);
        assertThat(result.unresolved()).contains(UNTRADEABLE_ITEM);
        assertThat(result.isUnresolved(UNTRADEABLE_ITEM)).isTrue();
    }

    @Test
    @DisplayName("untradeable item requested alone is unresolved rather than an exception")
    void untradeableItemAloneIsUnresolved() {
        UniversalisPrices result = client.getPrices(List.of(UNTRADEABLE_ITEM), DATA_CENTER);

        assertThat(result.prices()).isEmpty();
        assertThat(result.unresolved()).containsExactly(UNTRADEABLE_ITEM);
    }

    // ---------------------------------------------------------------------
    // The 100-item cap
    // ---------------------------------------------------------------------

    /**
     * The original bug returned exactly the first 100 ids and dropped the rest without error, so
     * the assertion looks for an id past the chunk boundary: one at or beyond 5100 can only have
     * come from a second request.
     */
    @Test
    @DisplayName("batches larger than the cap are chunked, not silently truncated")
    void largeBatchIsChunkedNotTruncated() {
        List<Integer> ids = IntStream.rangeClosed(5000, 5149).boxed().toList();

        UniversalisPrices result = client.getPrices(ids, DATA_CENTER);

        assertThat(result.prices().keySet())
                .as("ids past the chunk boundary must survive")
                .anySatisfy(id -> assertThat(id).isGreaterThanOrEqualTo(5100));

        assertThat(result.prices()).hasSizeGreaterThan(UNIVERSALIS_CAP);
    }

    /**
     * The core contract of the client: nothing you ask about disappears. This is the invariant
     * that makes silent truncation impossible to reintroduce unnoticed, and it is what lets the
     * cost calculator trust that a missing ingredient means something rather than nothing.
     */
    @Test
    @DisplayName("every requested id comes back as either priced or unresolved")
    void everyRequestedIdIsAccountedFor() {
        List<Integer> ids = IntStream.rangeClosed(5000, 5149).boxed().toList();

        UniversalisPrices result = client.getPrices(ids, DATA_CENTER);

        Set<Integer> accountedFor = new HashSet<>(result.prices().keySet());
        accountedFor.addAll(result.unresolved());

        assertThat(accountedFor).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("duplicate ids are collapsed rather than burning cap slots")
    void duplicateIdsAreDeduplicated() {
        UniversalisPrices result =
                client.getPrices(List.of(KNOWN_ITEM, KNOWN_ITEM, SECOND_ITEM, KNOWN_ITEM), DATA_CENTER);

        assertThat(result.prices()).containsOnlyKeys(KNOWN_ITEM, SECOND_ITEM);
    }

    // ---------------------------------------------------------------------
    // World and data centre lookups, used by the sync
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("worlds endpoint returns identifiable worlds")
    void worldsEndpointReturnsData() {
        List<UniversalisWorld> worlds = client.getWorlds();

        assertThat(worlds).isNotEmpty();
        assertThat(worlds).allSatisfy(world -> {
            assertThat(world.id()).isPositive();
            assertThat(world.name()).isNotBlank();
        });
        assertThat(worlds).anySatisfy(world -> assertThat(world.name()).isEqualTo(WORLD));
    }

    /**
     * The world-to-data-centre relationship only exists on this side of the API - a world does
     * not know its data centre - so an empty worlds array here would break the sync entirely.
     */
    @Test
    @DisplayName("data centres carry the world ids the sync needs to resolve relationships")
    void dataCentresCarryWorldIds() {
        List<UniversalisDataCenter> dataCenters = client.getDataCenters();

        assertThat(dataCenters).isNotEmpty();

        assertThat(dataCenters).allSatisfy(dc -> {
            assertThat(dc.name()).isNotBlank();
            assertThat(dc.worlds()).isNotNull();
        });
        assertThat(dataCenters).anySatisfy(dc -> assertThat(dc.name()).isEqualTo(DATA_CENTER));
    }
}