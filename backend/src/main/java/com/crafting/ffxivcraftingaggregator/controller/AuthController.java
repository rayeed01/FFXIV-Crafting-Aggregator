package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.AuthResponse;
import com.crafting.ffxivcraftingaggregator.domain.dto.LoginRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.RegisterRequest;
import com.crafting.ffxivcraftingaggregator.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration and sign-in.
 *
 * <p>Unauthenticated by necessity - these are how a caller obtains a token in the first place.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Creates an account and returns a token for it, so a new user is signed in immediately.
     *
     * <p>Requires a default world and data center, which is why the world endpoints are public:
     * the selector has to be populated before anyone has a token to fetch it with.
     *
     * @return 200 with the token, 400 on validation failure, 409 if the username or email is taken
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    /**
     * Exchanges credentials for a token.
     *
     * @return 200 with the token, or 401 with one message covering both an unknown username and a
     *         wrong password - distinguishing them would confirm which usernames exist
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        return ResponseEntity.ok(authService.login(loginRequest));
    }
}
