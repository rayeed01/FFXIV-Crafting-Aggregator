package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Items imported from XIVAPI, keyed internally by UUID but looked up by XIVAPI id wherever the
 * game's own identifiers are in play.
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    /** Uncapped substring search. Prefer the Top50 variant for anything user-facing. */
    List<Item> findByNameContainingIgnoreCase(String name);

    /**
     * Relevance-ranked substring search, for the search endpoint.
     *
     * <p>Matching stays a substring so "walnut" still finds "Claro Walnut Lumber", but results are
     * ranked rather than merely alphabetical. Plain alphabetical ordering made short queries
     * useless: "g" matched 7,536 items, and the first 50 alphabetically were all A's, so nothing
     * beginning with G ever appeared.
     *
     * <p>Four tiers, best first: an exact name, a name starting with the term, a name where some
     * later word starts with it, and finally the term appearing anywhere. Ties break on the
     * shorter name, so "Garlic" precedes "Garlic Cream Sauce", then alphabetically for stability.
     *
     * <p>The caller must supply the row limit as a {@link Pageable}, since JPQL has no LIMIT, and
     * must escape LIKE metacharacters in {@code q} first - see
     * {@code ItemServiceImpl.escapeLikeWildcards}. Unlike a derived Containing query, an explicit
     * LIKE does no escaping of its own, so a bare "%" would otherwise match every row.
     *
     * @param q already escaped for LIKE; the backslash escape character is declared by the query
     */
    @Query("""
        SELECT i FROM Item i
        WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
        ORDER BY
          CASE
            WHEN LOWER(i.name) = LOWER(:q) THEN 0
            WHEN LOWER(i.name) LIKE LOWER(CONCAT(:q, '%')) ESCAPE '\\' THEN 1
            WHEN LOWER(i.name) LIKE LOWER(CONCAT('% ', :q, '%')) ESCAPE '\\' THEN 2
            ELSE 3
          END,
          LENGTH(i.name),
          i.name
        """)
    List<Item> searchByRelevance(@Param("q") String q, Pageable pageable);

    Optional<Item> findByXivapiId(int xivapiId);

    /**
     * Bulk lookup by XIVAPI id, for resolving a whole batch of items in one query.
     *
     * <p>Ids with no matching row are simply absent from the result; callers that need to know
     * about them must compare against what they asked for.
     */
    List<Item> findByXivapiIdIn(Collection<Integer> xivapiIds);
}
