package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

/**
 * A freshly issued JWT.
 *
 * <p>Returned by both registration and sign-in, so a new account is usable without a second
 * round trip. The token carries the username as its subject; roles are not encoded in it and
 * are looked up per request.
 */
@Builder
public record AuthResponse(String token) {
}
