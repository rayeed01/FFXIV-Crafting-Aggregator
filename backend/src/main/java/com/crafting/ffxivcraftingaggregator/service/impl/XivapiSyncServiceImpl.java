package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.SyncStatus;
import com.crafting.ffxivcraftingaggregator.exception.SyncAlreadyRunningException;
import com.crafting.ffxivcraftingaggregator.service.XivapiSyncService;
import com.crafting.ffxivcraftingaggregator.sync.BulkSyncRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks the state of the bulk XIVAPI import and hands the work to {@link BulkSyncRunner}.
 *
 * <p>State lives in atomics because the run happens on another thread while status is polled from
 * request threads. The running flag is claimed with a compare-and-set rather than a check followed
 * by a write, so two simultaneous start requests cannot both begin an import.
 *
 * <p>Progress is in-memory only, so it does not survive a restart: a sync interrupted by one
 * leaves the flag cleared and its partial results in the database, and is simply run again.
 */
@Service
@RequiredArgsConstructor
public class XivapiSyncServiceImpl implements XivapiSyncService {

    private final BulkSyncRunner bulkSyncRunner;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger syncedCount = new AtomicInteger(0);
    private final AtomicReference<Instant> startedAt = new AtomicReference<>();
    private final AtomicReference<Instant> finishedAt = new AtomicReference<>();

    @Override
    public SyncStatus startBulkSync() {
        if(!running.compareAndSet(false,true)){
            throw new SyncAlreadyRunningException("A sync is already in progress");
        }
        syncedCount.set(0);
        startedAt.set(Instant.now());
        finishedAt.set(null);
        bulkSyncRunner.run(running, syncedCount,finishedAt);
        return new SyncStatus(true,0,startedAt.get(),null);
    }

    @Override
    public SyncStatus getStatus() {
        return new SyncStatus(running.get(), syncedCount.get(),startedAt.get(),finishedAt.get());
    }
}
