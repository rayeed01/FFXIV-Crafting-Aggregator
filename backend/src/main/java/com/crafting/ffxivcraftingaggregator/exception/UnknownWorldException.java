package com.crafting.ffxivcraftingaggregator.exception;

/**
 * A world name that does not match any synced world. Mapped to 400.
 *
 * <p>Distinct from the data not being synced at all, which is a 503 - one is the caller's mistake,
 * the other is the server not being ready.
 */
public class UnknownWorldException extends RuntimeException{
    public UnknownWorldException(String message){
        super(message);
    }
}
