package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.GameServerSyncResult;
import com.crafting.ffxivcraftingaggregator.domain.dto.SyncStatus;
import com.crafting.ffxivcraftingaggregator.service.GameServerSyncService;
import com.crafting.ffxivcraftingaggregator.service.XivapiSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data import triggers. Requires {@code ROLE_ADMIN}.
 *
 * <p>These populate the database from XIVAPI and Universalis. On a fresh install the world sync
 * must run first: world validation, pricing scopes and every client selector depend on it, and
 * the recipe import is of little use without somewhere to price the results.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final XivapiSyncService xivapiSyncService;
    private final GameServerSyncService gameServerSyncService;

    /**
     * Starts the bulk item and recipe import.
     *
     * <p>Returns 202 rather than 200: the work continues after the response, and progress is
     * polled from the companion GET.
     *
     * @return 202 with the initial status, or 409 if an import is already running
     */
    @PostMapping("/sync/recipe")
    public ResponseEntity<SyncStatus> triggerSync(){
        return ResponseEntity.accepted().body(xivapiSyncService.startBulkSync());
    }

    /** Current or most recent import status. Safe to poll while a run is in flight. */
    @GetMapping("/sync/recipe")
    public ResponseEntity<SyncStatus> getStatus(){
        return ResponseEntity.ok(xivapiSyncService.getStatus());
    }

    /**
     * Imports the world and data center list from Universalis.
     *
     * <p>Runs to completion before responding despite the 202, being only a few hundred rows.
     *
     * @return 202 with counts of what was written and skipped, or 502 if Universalis is unreachable
     */
    @PostMapping("/sync/worlds")
    public ResponseEntity<GameServerSyncResult> syncWorldsAndDc(){
        return ResponseEntity.accepted().body(gameServerSyncService.sync());
    }
}
