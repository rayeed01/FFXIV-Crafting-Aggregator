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

/**
 * Crafting lists belonging to the signed-in user.
 *
 * <p>The owner id always comes from the security context, never from the request body or path. A
 * client cannot name whose list it is operating on, which is what stops one user reaching
 * another's data.
 *
 * <p>A list owned by someone else is reported as 404, not 403: a 403 confirms the id exists and
 * would let an attacker enumerate other users' lists.
 */
@RestController
@RequestMapping("/api/v1/saved-crafts")
@RequiredArgsConstructor
public class SavedCraftController {

    private final SavedCraftService savedCraftService;

    /**
     * Creates a list. The recipe array may be empty; recipes are usually added afterwards.
     *
     * @return 200 with the created list, or 400 if the scope or a recipe id is invalid
     */
    @PostMapping
    public ResponseEntity<SavedCraftDto> createSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                          @Valid @RequestBody CreateSavedCraftRequest request){
        return  ResponseEntity.ok(savedCraftService.createSavedCraftRequest(userDetails.getId(), request));
    }

    /**
     * One list with its full recipe contents.
     *
     * @return 200 with the list, or 404 if it does not exist or belongs to another user
     */
    @GetMapping("/{savedCraftId}")
    public ResponseEntity<SavedCraftDto> getSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                       @PathVariable UUID savedCraftId){
        return ResponseEntity.ok(savedCraftService.getSavedCraft(userDetails.getId(), savedCraftId));
    }

    /**
     * Every list belonging to the current user.
     *
     * <p>Summaries only - a recipe count rather than the recipes - so an index page does not load
     * every list's full contents.
     */
    @GetMapping
    public ResponseEntity<List<SavedCraftSummaryDto>> getUserSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails){
        return ResponseEntity.ok(savedCraftService.getUserSavedCraft(userDetails.getId()));
    }

    /**
     * Updates title, notes and pricing scope. Contents are untouched.
     *
     * @return 200 with the updated list, 404 if not the caller's, 400 if the scope is invalid
     */
    @PatchMapping("/{savedCraftId}")
    public ResponseEntity<SavedCraftDto> updateSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                          @PathVariable UUID savedCraftId,
                                                          @Valid @RequestBody UpdateSavedCraftRequest request){
        return ResponseEntity.ok(savedCraftService.updateSavedCraft(userDetails.getId(),savedCraftId,request));
    }

    /** @return 204 on success, or 404 if the list does not exist or belongs to another user */
    @DeleteMapping("/{savedCraftId}")
    public ResponseEntity<Void> deleteSavedCraft(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                 @PathVariable UUID savedCraftId){
        savedCraftService.deleteSavedCraft(userDetails.getId(),savedCraftId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds recipes, or updates the quantity of ones already in the list.
     *
     * <p>Upserts rather than rejecting duplicates, which is how a client edits a quantity: re-send
     * the recipe id with a new one. There is deliberately no separate endpoint for that.
     *
     * @return 200 with the updated list, 404 if not the caller's, 400 if a recipe id or quantity is invalid
     */
    @PostMapping("/{savedCraftId}/recipes")
    public ResponseEntity<SavedCraftDto> addRecipes(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                    @PathVariable UUID savedCraftId,
                                                    @Valid @RequestBody AddRecipeRequest request){
        return ResponseEntity.ok(savedCraftService.addRecipes(userDetails.getId(),savedCraftId,request));
    }

    /**
     * Removes recipes. Ids not present are ignored rather than treated as an error.
     *
     * <p>Takes a body despite being a DELETE, so several recipes can be removed in one call.
     *
     * @return 200 with the updated list, or 404 if the list is not the caller's
     */
    @DeleteMapping("/{savedCraftId}/recipes")
    public ResponseEntity<SavedCraftDto> removeRecipes(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                       @PathVariable UUID savedCraftId,
                                                       @Valid @RequestBody RemoveRecipeRequest request){
        return ResponseEntity.ok(savedCraftService.removeRecipes(userDetails.getId(),savedCraftId,request));
    }

    /**
     * Prices every recipe in the list against the list's own scope.
     *
     * <p>The expensive endpoint here: it fans out to Universalis for every ingredient of every
     * recipe, which is why clients trigger it on demand rather than on page load.
     *
     * @return 200 with totals and a per-recipe breakdown, 404 if not the caller's, 502 if
     *         Universalis is unreachable
     */
    @GetMapping("/{savedCraftId}/cost")
    public ResponseEntity<SavedCraftCostDto> calculateCost(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                           @PathVariable UUID savedCraftId){
        return ResponseEntity.ok(savedCraftService.calculateCost(userDetails.getId(),savedCraftId));
    }
}
