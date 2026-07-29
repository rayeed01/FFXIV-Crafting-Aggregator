package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.UpdateUserRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;
import com.crafting.ffxivcraftingaggregator.exception.UserNotFoundException;
import com.crafting.ffxivcraftingaggregator.mapper.UserMapper;
import com.crafting.ffxivcraftingaggregator.repository.UserRepository;
import com.crafting.ffxivcraftingaggregator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    @Override
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return userMapper.toDto(user);
    }

    @Transactional
    @Override
    public UserDto updateDefaults(UUID id, UpdateUserRequest updateUserRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setDefaultWorld(updateUserRequest.defaultWorld());
        user.setDefaultDataCenter(updateUserRequest.defaultDataCenter());

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }
}
