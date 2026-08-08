package com.crafting.ffxivcraftingaggregator.exception;

/**
 * The world sync could not complete. Mapped to 502.
 *
 * <p>The failure is upstream at Universalis rather than here, and 502 says so.
 */
public class GameServerSyncException extends RuntimeException{
    public GameServerSyncException(String message){
        super(message);
    }

    public GameServerSyncException(String message,Throwable cause){
        super(message,cause);
    }
}
