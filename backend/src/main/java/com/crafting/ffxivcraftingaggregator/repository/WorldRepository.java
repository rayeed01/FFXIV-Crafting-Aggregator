package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.World;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Worlds, written by the Universalis sync.
 *
 * <p>A world always belongs to a data center, and almost every caller needs both together.
 */
@Repository
public interface WorldRepository extends JpaRepository<World, UUID> {

    /**
     * Every world with its data center already loaded, ordered by name.
     *
     * <p>The join fetch is the point: the association is lazy, so building a world list without it
     * triggers one query per world.
     */
    @Query("SELECT w FROM World w JOIN FETCH w.dataCenter ORDER BY w.name")
    List<World> findAllWithDataCenter();
}
