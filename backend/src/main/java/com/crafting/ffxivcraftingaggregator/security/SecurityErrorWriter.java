package com.crafting.ffxivcraftingaggregator.security;

import com.crafting.ffxivcraftingaggregator.domain.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Writes an {@link ErrorResponse} directly to the servlet response.
 *
 * <p>Shared by the authentication entry point and the access denied handler. Both run inside the
 * filter chain, outside the dispatcher, so they cannot use the usual message converters and have
 * to serialise by hand. Keeping that in one place is what makes their output identical to what
 * the global exception handler produces.
 *
 * <p>Package-private and non-instantiable: nothing outside the security package should be
 * bypassing the normal error path.
 */
final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    /**
     * Sets the status and writes a JSON error body.
     *
     * <p>Character encoding is set explicitly because a response committed from a filter does not
     * inherit the dispatcher's defaults, and a non-ASCII message would otherwise be mangled.
     *
     * @throws IOException if the response has already been committed
     */
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