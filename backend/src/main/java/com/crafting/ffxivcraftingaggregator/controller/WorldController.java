package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.DataCenterDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.WorldDto;
import com.crafting.ffxivcraftingaggregator.service.WorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public read access to the synced worlds and data centers.
 *
 * <p>Unauthenticated on purpose: registration requires a default world and data center, so the
 * selector has to be populated before anyone has a token to fetch it with.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorldController {

    private final WorldService worldService;

    @GetMapping("/worlds")
    public ResponseEntity<List<WorldDto>> getWorlds() {
        return ResponseEntity.ok(worldService.getAllWorlds());
    }

    @GetMapping("/data-centers")
    public ResponseEntity<List<DataCenterDto>> getDataCenters() {
        return ResponseEntity.ok(worldService.getAllDataCenters());
    }
}
