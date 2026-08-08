package com.crafting.ffxivcraftingaggregator.exception;

/**
 * No item exists for the requested id. Mapped to 404.
 */
public class ItemNotFoundException extends RuntimeException{
    public ItemNotFoundException(String message){
        super(message);
    }
}
