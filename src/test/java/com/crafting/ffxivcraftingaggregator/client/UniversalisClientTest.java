package com.crafting.ffxivcraftingaggregator.client;

import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPriceResponse;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.client.impl.UniversalisClientImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hits the real Universalis API. Excluded from `mvn test`; run with `mvn test -Pintegration`.
 *
 * <p>No Spring context and no database — the client is constructed directly, so this cannot
 * touch the seeded Postgres data.
 *
 * <p>Assertions are on SHAPE, never on specific prices. Market data changes between runs, so
 * asserting an exact gil value would produce a test that passes today and fails tomorrow.
 */
@Tag("integration")
@DisplayName("UniversalisClient against the live Universalis API")
class UniversalisClientTest {

    /** Base URL only — the impl appends /api/v2/... itself. Must match universalis.base-url. */
    private static final String BASE_URL = "https://universalis.app";

    private static final String WORLD = "Faerie";

    /** Commonly-listed cheap crafting mats; should have listings on any populated world. */
    private static final int KNOWN_ITEM = 5057;
    private static final int SECOND_ITEM = 5056;

    /** Gil. Exists in-game, never appears on the market board. */
    private static final int UNTRADEABLE_ITEM = 10155;

    /**
     * Declared locally rather than read from the impl. A test that asserts against the same
     * constant the production code uses would still pass if that constant were wrong.
     */
    private static final int UNIVERSALIS_CAP = 100;

    private static UniversalisClient client;

    @BeforeAll
    static void setUp() {
        client = new UniversalisClientImpl(RestClient.builder(), BASE_URL);
    }

    // ---------------------------------------------------------------------
    // Input guards — no network involved
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("empty id list short-circuits without touching the network")
    void emptyListReturnsEmptyResult() {
        UniversalisPrices result = client.getPrices(List.of(), WORLD);

        assertThat(result.prices()).isEmpty();
        assertThat(result.unresolved()).isEmpty();
    }

    @Test
    @DisplayName("null id list is tolerated rather than throwing")
    void nullListReturnsEmptyResult() {
        UniversalisPrices result = client.getPrices(null, WORLD);

        assertThat(result.prices()).isEmpty();
        assertThat(result.unresolved()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // The two response shapes
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("single id returns a correctly keyed entry with usable price data")
    void singleItemReturnsPriceData() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM), WORLD);

        assertThat(result.unresolved()).isEmpty();
        assertThat(result.prices()).containsKey(KNOWN_ITEM);

        UniversalisPriceResponse price = result.prices().get(KNOWN_ITEM);

        // The map key is built from the payload, not the request. If the @JsonProperty casing
        // on itemID ever broke, itemId would deserialise to 0 and every lookup would mis-key.
        assertThat(price.itemId()).isEqualTo(KNOWN_ITEM);
        assertThat(price.hasData()).isTrue();
        assertThat(price.minPrice()).isNotNull().isPositive();
        assertThat(price.lastUploadTime()).isNotNull();
    }

    @Test
    @DisplayName("batch call returns an entry for every requested id")
    void batchReturnsAllRequestedItems() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM, SECOND_ITEM), WORLD);

        assertThat(result.prices()).containsOnlyKeys(KNOWN_ITEM, SECOND_ITEM);
        assertThat(result.unresolved()).isEmpty();
    }

    @Test
    @DisplayName("single and batch paths agree for the same item")
    void singleAndBatchAgreeOnShape() {
        UniversalisPriceResponse fromSingle =
                client.getPrices(List.of(KNOWN_ITEM), WORLD).prices().get(KNOWN_ITEM);
        UniversalisPriceResponse fromBatch =
                client.getPrices(List.of(KNOWN_ITEM, SECOND_ITEM), WORLD).prices().get(KNOWN_ITEM);

        // Prices may legitimately differ if a listing sold between the two calls, so only the
        // fields that must be stable are compared.
        assertThat(fromBatch.itemId()).isEqualTo(fromSingle.itemId());
        assertThat(fromBatch.hasData()).isEqualTo(fromSingle.hasData());
    }

    // ---------------------------------------------------------------------
    // Unresolved items — the distinction a flat Map could not express
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("untradeable item in a batch is reported as unresolved, not merely absent")
    void untradeableItemIsUnresolvedInBatch() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM, UNTRADEABLE_ITEM), WORLD);

        assertThat(result.prices()).containsKey(KNOWN_ITEM);
        assertThat(result.prices()).doesNotContainKey(UNTRADEABLE_ITEM);
        assertThat(result.unresolved()).contains(UNTRADEABLE_ITEM);
        assertThat(result.isUnresolved(UNTRADEABLE_ITEM)).isTrue();
    }

    @Test
    @DisplayName("untradeable item requested alone is unresolved, not an exception")
    void untradeableItemAloneIsUnresolved() {
        // The single-item endpoint has no unresolvedItems field, so Universalis answers 404.
        // The impl translates that into the same signal the batch path produces, so callers
        // see one consistent model regardless of how many ids they asked for.
        UniversalisPrices result = client.getPrices(List.of(UNTRADEABLE_ITEM), WORLD);

        assertThat(result.prices()).isEmpty();
        assertThat(result.unresolved()).containsExactly(UNTRADEABLE_ITEM);
    }

    @Test
    @DisplayName("minPriceFor is empty for an unresolved item rather than zero")
    void minPriceForUnresolvedItemIsEmpty() {
        UniversalisPrices result = client.getPrices(List.of(KNOWN_ITEM, UNTRADEABLE_ITEM), WORLD);

        // The whole point of Optional here: an unbuyable item must never read as free.
        assertThat(result.minPriceFor(UNTRADEABLE_ITEM)).isEmpty();
        assertThat(result.minPriceFor(KNOWN_ITEM)).isPresent();
    }

    // ---------------------------------------------------------------------
    // The 100-item cap
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("batches larger than the cap are chunked, not silently truncated")
    void largeBatchIsChunkedNotTruncated() {
        List<Integer> ids = IntStream.rangeClosed(5000, 5149).boxed().toList();

        UniversalisPrices result = client.getPrices(ids, WORLD);

        // The original bug returned exactly the first 100 ids and dropped the rest without
        // error. An id at or beyond 5100 can only have come from a second request.
        assertThat(result.prices().keySet())
                .anySatisfy(id -> assertThat(id).isGreaterThanOrEqualTo(5100));

        assertThat(result.prices()).hasSizeGreaterThan(UNIVERSALIS_CAP);
    }

    @Test
    @DisplayName("every requested id is accounted for as either priced or unresolved")
    void everyRequestedIdIsAccountedFor() {
        List<Integer> ids = IntStream.rangeClosed(5000, 5149).boxed().toList();

        UniversalisPrices result = client.getPrices(ids, WORLD);

        Set<Integer> accountedFor = new HashSet<>(result.prices().keySet());
        accountedFor.addAll(result.unresolved());

        // This is the core contract of the client: nothing you ask about disappears. It is the
        // invariant that makes silent truncation impossible to reintroduce unnoticed.
        assertThat(accountedFor).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("duplicate ids are collapsed rather than burning cap slots")
    void duplicateIdsAreDeduplicated() {
        UniversalisPrices result =
                client.getPrices(List.of(KNOWN_ITEM, KNOWN_ITEM, SECOND_ITEM, KNOWN_ITEM), WORLD);

        assertThat(result.prices()).containsOnlyKeys(KNOWN_ITEM, SECOND_ITEM);
    }

    // ---------------------------------------------------------------------
    // Known gap — documents why WorldRegistry validation is needed
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("KNOWN GAP: an invalid world behaves inconsistently across the two paths")
    void invalidWorldIsNotDistinguishedFromAnUnknownItem() {
        String badWorld = "Faeriee";

        // Single path: Universalis 404s, the impl catches it, and the item looks unresolved —
        // i.e. "this cannot be bought anywhere", which is wrong and silent.
        UniversalisPrices single = client.getPrices(List.of(KNOWN_ITEM), badWorld);
        assertThat(single.unresolved()).contains(KNOWN_ITEM);

        // Batch path: the same 404 is not caught and propagates instead.
        assertThatThrownBy(() -> client.getPrices(List.of(KNOWN_ITEM, SECOND_ITEM), badWorld))
                .isInstanceOf(RestClientResponseException.class);

        // Two behaviors for one user error, and neither says "unknown world". The fix belongs
        // upstream: validate the world/DC against /api/v2/worlds before any price lookup.
        // Delete this test once that validation exists.
    }
}