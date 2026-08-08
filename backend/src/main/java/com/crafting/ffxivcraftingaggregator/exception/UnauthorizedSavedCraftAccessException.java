package com.crafting.ffxivcraftingaggregator.exception;

/**
 * A crafting list was reached by someone other than its owner.
 *
 * <p>Mapped to 404 rather than 403 on purpose. A 403 confirms the resource exists, which is enough
 * to enumerate other users' lists by trying ids.
 */
public class UnauthorizedSavedCraftAccessException extends RuntimeException{
    public UnauthorizedSavedCraftAccessException(String message){
        super(message);
    }
}
