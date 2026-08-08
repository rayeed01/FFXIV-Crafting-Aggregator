package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.UpdateUserRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.UserDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;
import com.crafting.ffxivcraftingaggregator.exception.UserNotFoundException;
import com.crafting.ffxivcraftingaggregator.mapper.UserMapper;
import com.crafting.ffxivcraftingaggregator.repository.UserRepository;
import com.crafting.ffxivcraftingaggregator.service.UserService;
import com.crafting.ffxivcraftingaggregator.service.WorldRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * Profile reads and default-market updates.
 *
 * <p>Names are canonicalised and validated as a pair before being stored, so a saved default is
 * always a market that actually exists.
 */
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final WorldRegistry worldRegistry;

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

        String world = worldRegistry.canonicalWorldName(updateUserRequest.defaultWorld());
        String dataCenter = worldRegistry.canonicalDataCenterName(updateUserRequest.defaultDataCenter());

        user.setDefaultWorld(world);
        user.setDefaultDataCenter(dataCenter);

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }
}
