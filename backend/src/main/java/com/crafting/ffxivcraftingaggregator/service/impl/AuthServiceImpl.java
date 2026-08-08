package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.AuthResponse;
import com.crafting.ffxivcraftingaggregator.domain.dto.LoginRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.RegisterRequest;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;
import com.crafting.ffxivcraftingaggregator.repository.UserRepository;
import com.crafting.ffxivcraftingaggregator.security.FfxivUserDetails;
import com.crafting.ffxivcraftingaggregator.security.JwtService;
import com.crafting.ffxivcraftingaggregator.service.AuthService;
import com.crafting.ffxivcraftingaggregator.service.WorldRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
/**
 * Password authentication issuing JWTs.
 *
 * <p>Passwords are stored as bcrypt hashes through the injected encoder and are never held or
 * logged in plain form. Sign-in delegates to Spring Security's {@code AuthenticationManager}
 * rather than comparing hashes here, so the configured encoder and user lookup stay the single
 * source of truth.
 *
 * <p>World and data center names are canonicalised at registration, so a stored default is always
 * in the exact form Universalis expects.
 */
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WorldRegistry worldRegistry;


    @Transactional
    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        if(userRepository.findByUsername(registerRequest.username()).isPresent()){
            throw new IllegalArgumentException("Username already taken");
        }

        String world = worldRegistry.canonicalWorldName(registerRequest.defaultWorld());
        String dataCenter = worldRegistry.canonicalDataCenterName(registerRequest.defaultDataCenter());

        User user = User.builder()
                .username(registerRequest.username())
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .defaultDataCenter(dataCenter)
                .defaultWorld(world)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(new FfxivUserDetails(user));
        return new AuthResponse(token);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authResult = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        UserDetails userDetails = Objects.requireNonNull((UserDetails)authResult.getPrincipal());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token);
    }
}
