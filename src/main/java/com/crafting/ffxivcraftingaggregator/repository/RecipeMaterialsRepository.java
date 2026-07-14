package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.RecipeMaterials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecipeMaterialsRepository extends JpaRepository<RecipeMaterials, UUID> {
}
