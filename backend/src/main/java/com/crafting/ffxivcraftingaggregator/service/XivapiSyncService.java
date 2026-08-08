package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.SyncStatus;

public interface XivapiSyncService {
    SyncStatus startBulkSync();
    SyncStatus getStatus();
}
