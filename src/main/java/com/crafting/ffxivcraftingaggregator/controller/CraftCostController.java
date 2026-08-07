package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.CraftCostNode;
import com.crafting.ffxivcraftingaggregator.service.CraftCostService;
import com.crafting.ffxivcraftingaggregator.service.WorldRegistry;
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

    @GetMapping("/{itemXivapiId}")
    public ResponseEntity<CraftCostNode> calculate(
            @PathVariable int itemXivapiId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam @NotBlank String scope) {

        String canonicalScope = resolveScope(scope);

        return ResponseEntity.ok(craftCostService.calculate(itemXivapiId, quantity, canonicalScope));
    }

    private String resolveScope(String scope) {
        try {
            return worldRegistry.canonicalWorldName(scope);
        } catch (RuntimeException ex) {
            return worldRegistry.canonicalDataCenterName(scope);
        }
    }
}