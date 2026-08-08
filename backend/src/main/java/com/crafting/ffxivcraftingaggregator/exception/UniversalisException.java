package com.crafting.ffxivcraftingaggregator.exception;

/**
 * A market price request to Universalis failed. Mapped to 502.
 *
 * <p>Reported as an upstream failure rather than a server error, because grouping it with genuine
 * bugs made an outage at Universalis look like a fault in this application.
 */
public class UniversalisException extends RuntimeException{
    public UniversalisException(String message){
        super(message);
    }

    public UniversalisException(String message, Throwable cause){
        super(message,cause);
    }
}
