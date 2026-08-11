package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.domain.entity.Recipe;
import io.lettuce.core.dynamic.annotation.Param;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recipes imported from XIVAPI, together with the queries that make tree traversal affordable.
 */
@NullMarked
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    /** One recipe with its result item and every ingredient already loaded. */
    @EntityGraph(attributePaths = {"resultItem", "recipeIngredients", "recipeIngredients.item"})
    @Override
    Optional<Recipe> findById(UUID id);

    @EntityGraph(attributePaths = {"resultItem"})
    List<Recipe> findByResultItem_NameContainingIgnoreCase(String name);

    /**
     * Relevance-ranked search on the result item's name.
     *
     * <p>Mirrors {@code ItemRepository.searchByRelevance} exactly - same four tiers, same
     * tie-breaks - so the two halves of a merged search result agree on what "most relevant"
     * means. See that method for why ranking replaced plain alphabetical ordering.
     *
     * <p>Joins rather than using an entity graph, because the ordering references the joined
     * item's name and the join has to be explicit for that.
     *
     * @param q already escaped for LIKE; see {@code RecipeServiceImpl.escapeLikeWildcards}
     */
    @Query("""
        SELECT r FROM Recipe r
        JOIN FETCH r.resultItem item
        WHERE LOWER(item.name) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
        ORDER BY
          CASE
            WHEN LOWER(item.name) = LOWER(:q) THEN 0
            WHEN LOWER(item.name) LIKE LOWER(CONCAT(:q, '%')) ESCAPE '\\' THEN 1
            WHEN LOWER(item.name) LIKE LOWER(CONCAT('% ', :q, '%')) ESCAPE '\\' THEN 2
            ELSE 3
          END,
          LENGTH(item.name),
          item.name
        """)
    List<Recipe> searchByRelevance(@Param("q") String q, Pageable pageable);

    /** @param job an XIVAPI CraftType such as "Smithing", not a job name such as "Blacksmith" */
    @EntityGraph(attributePaths = {"resultItem"})
    List<Recipe> findByJobIgnoreCase(String job);

    Optional<Recipe> findByResultItem(Item item);

    Optional<Recipe> findByXivapiId(int xivapiId);

    /**
     * Flags every item that some recipe produces as craftable, in one statement.
     *
     * <p>Run once at the end of a sync rather than per row: the flag is a denormalisation of "a
     * recipe exists for this item", and maintaining it during the import would mean an update per
     * recipe. It also cannot be set correctly until every recipe has landed.
     */
    @Transactional
    @Modifying
    @Query("UPDATE Item i SET i.canBeCrafted = true where i.id IN (SELECT r.resultItem.id from Recipe r)")
    void markResultItemsCraftable();

    /**
     * Every recipe producing any of the given items, with result item and ingredients loaded.
     *
     * <p>This is what lets the cost tree be walked one level at a time: a whole tier of the tree
     * is resolved in a single query, rather than one per item.
     */
    @Query("""
        SELECT DISTINCT r FROM Recipe r
        JOIN FETCH r.resultItem
        JOIN FETCH r.recipeIngredients ri
        JOIN FETCH ri.item
        WHERE r.resultItem.xivapiId IN :itemIds
        """)
    List<Recipe> findByResultItemXivapiIdInWithIngredients(@Param("itemIds") Collection<Integer> itemIds);
}
