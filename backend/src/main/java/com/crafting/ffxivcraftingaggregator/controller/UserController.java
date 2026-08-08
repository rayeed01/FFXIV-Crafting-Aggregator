package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.UpdateUserRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;
import com.crafting.ffxivcraftingaggregator.security.FfxivUserDetails;
import com.crafting.ffxivcraftingaggregator.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The signed-in user's own profile.
 *
 * <p>Everything here is scoped to the authenticated principal - there is deliberately no endpoint
 * for fetching an arbitrary user by id, so no caller can read another account.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * The current user's profile.
     *
     * <p>Includes {@code role}, which clients use to decide whether to show admin navigation.
     * That is presentation only; the admin endpoints enforce the role themselves regardless.
     *
     * @return 200 with the profile, or 401 if unauthenticated
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getUserById(@AuthenticationPrincipal FfxivUserDetails userDetails){
        return  ResponseEntity.ok(userService.getUserById(userDetails.getId()));
    }

    /**
     * Updates the default data center and world.
     *
     * <p>These only pre-fill the pricing scope for new work; existing lists keep their own scope.
     *
     * @return 200 with the updated profile, 400 if the world and data center do not match
     */
    @PatchMapping("/me/defaults")
    public ResponseEntity<UserDto> updateDefaults(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                  @Valid @RequestBody UpdateUserRequest request){
        return ResponseEntity.ok(userService.updateDefaults(userDetails.getId(), request));
    }

}
