package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.UpdateUserRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;

import java.util.UUID;

public interface UserService {

    UserDto getUserById(UUID id);
    UserDto updateDefaults(UUID id, UpdateUserRequest updateUserRequest);
}
