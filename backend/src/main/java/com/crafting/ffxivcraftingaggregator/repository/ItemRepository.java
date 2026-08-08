package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findByNameContainingIgnoreCase(String name);

    /**
     * Capped, ordered variant used by the search endpoint.
     *
     * <p>The uncapped version above returns every match, which for a one-letter query is ~7,000
     * rows and 1.8 MB of JSON - enough to make the search box feel laggy while the client parses
     * and renders it. Nobody scrolls past the first screen of a substring search, so the cap
     * costs nothing and the ordering makes the truncation deterministic rather than arbitrary.
     */
    List<Item> findTop50ByNameContainingIgnoreCaseOrderByNameAsc(String name);
    Optional<Item> findByXivapiId(int xivapiId);
    List<Item> findByXivapiIdIn(Collection<Integer> xivapiIds);
}
