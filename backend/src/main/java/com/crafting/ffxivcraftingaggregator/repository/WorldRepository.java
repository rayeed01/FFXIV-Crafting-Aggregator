package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.World;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorldRepository extends JpaRepository<World, UUID> {

    @Query("SELECT w FROM World w JOIN FETCH w.dataCenter ORDER BY w.name")
    List<World> findAllWithDataCenter();
}
