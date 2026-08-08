package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.CraftCostNode;
import com.crafting.ffxivcraftingaggregator.domain.dto.Quality;
import com.crafting.ffxivcraftingaggregator.service.CraftCostService;
import com.crafting.ffxivcraftingaggregator.service.WorldRegistry;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/craft-cost")
@Validated
public class CraftCostController {

    private final CraftCostService craftCostService;
    private final WorldRegistry worldRegistry;

    public CraftCostController(CraftCostService craftCostService,
                               WorldRegistry worldRegistry) {
        this.craftCostService = craftCostService;
        this.worldRegistry = worldRegistry;
    }

    /**
     * @param scope   a world or data center name; either is accepted and canonicalised
     * @param quality which listing to price the requested item against, defaulting to whichever
     *                is cheaper. Applies to this item only, not to its ingredients.
     */
    @GetMapping("/{itemXivapiId}")
    public ResponseEntity<CraftCostNode> calculate(
            @PathVariable int itemXivapiId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 999, message = "Quantity must be 999 or less") int quantity,
            @RequestParam @NotBlank String scope,
            @RequestParam(defaultValue = "CHEAPEST") Quality quality) {

        String canonicalScope = resolveScope(scope);

        return ResponseEntity.ok(craftCostService.calculate(itemXivapiId, quantity, canonicalScope, quality));
    }

    /**
     * A scope may be either a world or a data center. Worlds are tried first; the data center
     * lookup is the fallback, and its exception is the one that surfaces when neither matches.
     */
    private String resolveScope(String scope) {
        try {
            return worldRegistry.canonicalWorldName(scope);
        } catch (RuntimeException ex) {
            return worldRegistry.canonicalDataCenterName(scope);
        }
    }
}
