package com.crafting.ffxivcraftingaggregator.domain.dto;

/**
 * Outcome of a world and data center import.
 *
 * <p>{@code worldsSkipped} counts worlds Universalis returned whose data center could not be
 * resolved. Those are skipped rather than failing the run, so a non-zero count here is a
 * warning worth looking at rather than an error.
 */
public record GameServerSyncResult(int dataCentersSynced,
                                   int worldsSynced,
                                   int worldsSkipped) {
}
