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

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final XivapiSyncService xivapiSyncService;
    private final GameServerSyncService gameServerSyncService;

    @PostMapping("/sync/recipe")
    public ResponseEntity<SyncStatus> triggerSync(){
        return ResponseEntity.accepted().body(xivapiSyncService.startBulkSync());
    }

    @GetMapping("/sync/recipe")
    public ResponseEntity<SyncStatus> getStatus(){
        return ResponseEntity.ok(xivapiSyncService.getStatus());
    }

    @PostMapping("/sync/worlds")
    public ResponseEntity<GameServerSyncResult> syncWorldsAndDc(){
        return ResponseEntity.accepted().body(gameServerSyncService.sync());
    }
}
