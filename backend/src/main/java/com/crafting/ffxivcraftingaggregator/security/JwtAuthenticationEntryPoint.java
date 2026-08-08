package com.crafting.ffxivcraftingaggregator.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Responds to unauthenticated requests for a protected resource.
 *
 * <p>Exists so a missing or expired token produces the same error shape as every other failure.
 * Without it those requests return Spring Security's default body, and a client would need two
 * parsers for one API.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {

        SecurityErrorWriter.write(response, objectMapper,
                HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}