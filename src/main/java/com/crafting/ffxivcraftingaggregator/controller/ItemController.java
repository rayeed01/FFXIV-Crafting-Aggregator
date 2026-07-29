package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> findItemById(@PathVariable UUID id){
        return ResponseEntity.ok(itemService.findItemById(id));

    }

    @GetMapping
    public ResponseEntity<List<ItemDto>> searchItems(@RequestParam String search){
        return ResponseEntity.ok(itemService.searchItems(search));

    }
}
