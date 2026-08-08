package com.crafting.ffxivcraftingaggregator.mapper.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;
import com.crafting.ffxivcraftingaggregator.mapper.UserMapper;
import org.springframework.stereotype.Component;

/**
 * Builds a profile response.
 *
 * <p>Copies fields individually rather than reflectively, so the password hash cannot be included
 * by accident when a field is added.
 */
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
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
