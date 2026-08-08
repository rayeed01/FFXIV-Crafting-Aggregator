package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.UpdateUserRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;

import java.util.UUID;

/** Profile reads and preference updates for an existing account. */
public interface UserService {

    /**
     * @throws com.crafting.ffxivcraftingaggregator.exception.UserNotFoundException if no such user
     */
    UserDto getUserById(UUID id);

    /**
     * Changes the user's default data center and world.
     *
     * <p>These only pre-fill the pricing scope in clients; nothing already saved is re-priced.
     * The pair is validated the same way as at registration.
     *
     * @throws com.crafting.ffxivcraftingaggregator.exception.UserNotFoundException if no such user
     * @throws com.crafting.ffxivcraftingaggregator.exception.WorldDataCenterMismatchException
     *         if the world does not belong to the given data center
     */
    UserDto updateDefaults(UUID id, UpdateUserRequest updateUserRequest);
}
