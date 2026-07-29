package com.crafting.ffxivcraftingaggregator.domain.dto;

import java.time.Instant;

public record SyncStatus(
        boolean running,
        int syncedCount,
        Instant startedAt,
        Instant finishedAt) {
}
