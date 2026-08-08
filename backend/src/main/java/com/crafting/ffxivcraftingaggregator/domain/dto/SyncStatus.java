package com.crafting.ffxivcraftingaggregator.domain.dto;

import java.time.Instant;

/**
 * Progress of the bulk XIVAPI import.
 *
 * <p>Held in memory, so it does not survive a restart: a sync interrupted by one reports as not
 * running, with its partial results already in the database.
 *
 * <p>{@code startedAt} is null before the first run of the process; {@code finishedAt} is null
 * while a run is in flight.
 */
public record SyncStatus(
        boolean running,
        int syncedCount,
        Instant startedAt,
        Instant finishedAt) {
}
