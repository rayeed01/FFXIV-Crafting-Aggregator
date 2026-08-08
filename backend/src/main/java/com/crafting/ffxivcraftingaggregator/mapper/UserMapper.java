package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;

/**
 * Converts a {@link User} entity into a profile response.
 *
 * <p>Deliberately omits the password hash. Nothing that reaches the API boundary should carry it.
 */
public interface UserMapper {
    UserDto toDto(User user);
}
