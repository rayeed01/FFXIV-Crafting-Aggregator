package com.crafting.ffxivcraftingaggregator.exception;

/**
 * The world tables are empty, so nothing can be validated or priced yet. Mapped to 503.
 *
 * <p>503 rather than 404: the caller's world name was very likely fine, and the server simply is
 * not ready. A 404 sends someone hunting for a typo in a value that was correct all along.
 */
public class GameServerDataNotSyncedException extends RuntimeException{
    public GameServerDataNotSyncedException(String message){
        super(message);
    }
}
