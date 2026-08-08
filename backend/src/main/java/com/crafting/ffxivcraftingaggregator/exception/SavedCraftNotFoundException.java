package com.crafting.ffxivcraftingaggregator.exception;

/**
 * No such crafting list. Mapped to 404.
 *
 * <p>Also thrown when a list exists but belongs to another user. Keep the message neutral: the
 * whole point of using 404 there is not to confirm that the id is real.
 */
public class SavedCraftNotFoundException extends RuntimeException{
    public SavedCraftNotFoundException(String message){
        super(message);
    }
}
