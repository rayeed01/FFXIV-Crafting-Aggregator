package com.crafting.ffxivcraftingaggregator.exception;

public class UniversalisException extends RuntimeException{
    public UniversalisException(String message){
        super(message);
    }

    public UniversalisException(String message, Throwable cause){
        super(message,cause);
    }
}
