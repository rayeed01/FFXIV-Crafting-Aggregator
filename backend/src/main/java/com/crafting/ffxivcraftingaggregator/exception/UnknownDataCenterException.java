package com.crafting.ffxivcraftingaggregator.exception;

/**
 * A data center name that does not match any synced data center. Mapped to 400.
 */
public class UnknownDataCenterException extends RuntimeException{
    public UnknownDataCenterException(String message){
        super(message);
    }
}
