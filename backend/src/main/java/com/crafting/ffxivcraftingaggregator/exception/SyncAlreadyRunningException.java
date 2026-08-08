package com.crafting.ffxivcraftingaggregator.exception;

public class SyncAlreadyRunningException extends RuntimeException{
    public SyncAlreadyRunningException(String message){
        super(message);
    }
}
