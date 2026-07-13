package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

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
