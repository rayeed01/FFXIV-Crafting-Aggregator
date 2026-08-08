package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.client.dto.SavedCraftCostDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.*;
import com.crafting.ffxivcraftingaggregator.security.FfxivUserDetails;
import com.crafting.ffxivcraftingaggregator.service.SavedCraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/saved-crafts")
@RequiredArgsConstructor
public class SavedCraftController {

    private final SavedCraftService savedCraftService;

    @PostMapping
    public ResponseEntity<SavedCraftDto> createSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                          @Valid @RequestBody CreateSavedCraftRequest request){
        return  ResponseEntity.ok(savedCraftService.createSavedCraftRequest(userDetails.getId(), request));
    }

    @GetMapping("/{savedCraftId}")
    public ResponseEntity<SavedCraftDto> getSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                       @PathVariable UUID savedCraftId){
        return ResponseEntity.ok(savedCraftService.getSavedCraft(userDetails.getId(), savedCraftId));
    }

    @GetMapping
    public ResponseEntity<List<SavedCraftSummaryDto>> getUserSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails){
        return ResponseEntity.ok(savedCraftService.getUserSavedCraft(userDetails.getId()));
    }

    @PatchMapping("/{savedCraftId}")
    public ResponseEntity<SavedCraftDto> updateSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                          @PathVariable UUID savedCraftId,
                                                          @Valid @RequestBody UpdateSavedCraftRequest request){
        return ResponseEntity.ok(savedCraftService.updateSavedCraft(userDetails.getId(),savedCraftId,request));
    }

    @DeleteMapping("/{savedCraftId}")
    public ResponseEntity<Void> deleteSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                 @PathVariable UUID savedCraftId){
        savedCraftService.deleteSavedCraft(userDetails.getId(),savedCraftId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{savedCraftId}/recipes")
    public ResponseEntity<SavedCraftDto> addRecipes(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                    @PathVariable UUID savedCraftId,
                                                    @Valid @RequestBody AddRecipeRequest request){
        return ResponseEntity.ok(savedCraftService.addRecipes(userDetails.getId(),savedCraftId,request));
    }

    @DeleteMapping("/{savedCraftId}/recipes")
    public ResponseEntity<SavedCraftDto> removeRecipes(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                       @PathVariable UUID savedCraftId,
                                                       @Valid @RequestBody RemoveRecipeRequest request){
        return ResponseEntity.ok(savedCraftService.removeRecipes(userDetails.getId(),savedCraftId,request));
    }

    @GetMapping("/{savedCraftId}/cost")
    public ResponseEntity<SavedCraftCostDto> calculateCost(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                           @PathVariable UUID savedCraftId){
        return ResponseEntity.ok(savedCraftService.calculateCost(userDetails.getId(),savedCraftId));
    }
}
