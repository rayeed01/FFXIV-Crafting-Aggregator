package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.DataCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Data centers, written by the Universalis world sync.
 *
 * <p>Only the inherited methods are needed - there are around twenty rows, and callers either
 * want all of them or reach one through a {@code World}.
 */
@Repository
public interface DataCenterRepository extends JpaRepository<DataCenter, UUID> {
}
