package com.crafting.ffxivcraftingaggregator.domain.dto;

public record GameServerSyncResult(int dataCentersSynced,
                                   int worldsSynced,
                                   int worldsSkipped) {
}
