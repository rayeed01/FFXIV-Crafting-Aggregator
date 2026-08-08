package com.crafting.ffxivcraftingaggregator.security;

import com.crafting.ffxivcraftingaggregator.domain.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(HttpServletResponse response,
                      ObjectMapper objectMapper,
                      HttpStatus status,
                      String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                objectMapper.writeValueAsString(new ErrorResponse(status.value(), message)));
    }
}