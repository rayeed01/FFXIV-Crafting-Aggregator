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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getUserById(@AuthenticationPrincipal FfxivUserDetails userDetails){
        return  ResponseEntity.ok(userService.getUserById(userDetails.getId()));
    }

    @PatchMapping("/me/defaults")
    public ResponseEntity<UserDto> updateDefaults(@AuthenticationPrincipal FfxivUserDetails userDetails,
                                                  @Valid @RequestBody UpdateUserRequest request){
        return ResponseEntity.ok(userService.updateDefaults(userDetails.getId(), request));
    }

}
