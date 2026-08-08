package com.crafting.ffxivcraftingaggregator.exception;

/**
 * No user exists for the requested id. Mapped to 404.
 *
 * <p>Reaching this from an authenticated request means a token outlived the account it names.
 */
public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(String message){
        super(message);
    }
}
