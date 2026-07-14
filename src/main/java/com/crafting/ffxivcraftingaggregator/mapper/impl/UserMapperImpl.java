package com.crafting.ffxivcraftingaggregator.mapper.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;
import com.crafting.ffxivcraftingaggregator.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements UserMapper {
    @Override
    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .defaultDataCenter(user.getDefaultDataCenter())
                .defaultWorld(user.getDefaultWorld())
                .build();
    }
}
