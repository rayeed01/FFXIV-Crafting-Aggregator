package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public read access to the item catalogue.
 *
 * <p>Unauthenticated because this is game data XIVAPI already serves openly, and because browsing
 * is useful before anyone signs up.
 */
@RestController
@RequestMapping(path = "/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * @param id this application's item id, not the XIVAPI one
     * @return 200 with the item, 404 if unknown, 400 if the id is not a UUID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> findItemById(@PathVariable UUID id){
        return ResponseEntity.ok(itemService.findItemById(id));

    }

    /**
     * Substring search on item name.
     *
     * <p>Results are capped server-side, so a very short query returns a usable page rather than
     * thousands of rows. Callers wanting more should narrow the term.
     *
     * @return 200 with matches, possibly empty; 400 if {@code search} is absent
     */
    @GetMapping
    public ResponseEntity<List<ItemDto>> searchItems(@RequestParam String search){
        return ResponseEntity.ok(itemService.searchItems(search));

    }
}
