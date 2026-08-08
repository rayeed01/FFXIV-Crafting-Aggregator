package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.AuthResponse;
import com.crafting.ffxivcraftingaggregator.domain.dto.LoginRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
}
