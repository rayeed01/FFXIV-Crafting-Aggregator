package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.SyncStatus;

/**
 * Bulk import of the item and recipe catalogue from XIVAPI.
 *
 * <p>The import takes long enough that it cannot be a synchronous request, so it runs in the
 * background and progress is polled. Only one run is allowed at a time.
 */
public interface XivapiSyncService {

    /**
     * Starts an import and returns immediately with its initial status.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.SyncAlreadyRunningException
     *         if an import is already in flight
     */
    SyncStatus startBulkSync();

    /** Current or most recent import status; safe to poll. */
    SyncStatus getStatus();
}
