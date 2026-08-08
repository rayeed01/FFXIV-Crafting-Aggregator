package com.crafting.ffxivcraftingaggregator.exception;

/**
 * A sync was requested while one is already in flight. Mapped to 409.
 *
 * <p>Concurrent imports would duplicate upstream calls and race on the same rows, so only one runs
 * at a time.
 */
public class SyncAlreadyRunningException extends RuntimeException{
    public SyncAlreadyRunningException(String message){
        super(message);
    }
}
