package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.Craft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CraftRepository extends JpaRepository<Craft,Long> {
}
