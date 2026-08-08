package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.AuthResponse;
import com.crafting.ffxivcraftingaggregator.domain.dto.LoginRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.RegisterRequest;

/**
 * Registration and sign-in.
 *
 * <p>Both operations return a signed JWT directly, so a new account is usable without a second
 * round trip to log in.
 */
public interface AuthService {

    /**
     * Creates an account and issues a token for it.
     *
     * <p>The requested world and data center are validated as a pair before the user is stored;
     * a default market that does not exist would fail later at pricing time instead.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException if the username or email is taken
     * @throws com.crafting.ffxivcraftingaggregator.exception.UnknownWorldException if the world is not recognised
     * @throws com.crafting.ffxivcraftingaggregator.exception.WorldDataCenterMismatchException
     *         if the world does not belong to the given data center
     */
    AuthResponse register(RegisterRequest registerRequest);

    /**
     * Verifies credentials and issues a token.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         for both an unknown username and a wrong password, deliberately indistinguishable
     */
    AuthResponse login(LoginRequest loginRequest);
}
