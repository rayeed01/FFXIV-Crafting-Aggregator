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
    Optional<Item> findByXivapiId(int xivapiId);
    List<Item> findByXivapiIdIn(Collection<Integer> xivapiIds);
}
