package com.crafting.ffxivcraftingaggregator.mapper;

import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;

public interface UserMapper {
    UserDto toDto(User user);
}
