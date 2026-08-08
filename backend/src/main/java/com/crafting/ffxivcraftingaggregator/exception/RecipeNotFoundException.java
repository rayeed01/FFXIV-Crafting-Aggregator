package com.crafting.ffxivcraftingaggregator.exception;

/**
 * No recipe exists for the requested id. Mapped to 404.
 */
public class RecipeNotFoundException extends RuntimeException{
    public RecipeNotFoundException(String message){
        super(message);
    }
}
