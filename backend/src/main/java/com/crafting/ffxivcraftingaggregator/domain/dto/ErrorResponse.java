package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * The response body for every handled failure.
 *
 * <p>One shape for all errors, so clients need only one parser. {@code errors} is populated for
 * validation failures and empty otherwise, letting a form mark the exact offending input
 * instead of showing a generic message.
 *
 * <p>Not guaranteed for authentication failures raised inside the JWT filter: those run before
 * the dispatcher and bypass the handler that produces this shape.
 */
@Builder
public record ErrorResponse(int status,
                            String message,
                            Instant timeStamp,
                            List<FieldError> errors) {

    public  record FieldError(String field, String message){}

    public ErrorResponse(int status, String message){
        this(status, message, Instant.now(), List.of());
    }

    public ErrorResponse(int status, String message, List<FieldError> errors){
        this(status, message, Instant.now(), errors);
    }
}
