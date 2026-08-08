package com.crafting.ffxivcraftingaggregator.exception;

/**
 * A world and data center that are each valid but do not belong together. Mapped to 400.
 *
 * <p>Worth its own type because the pair is checkable only as a pair: neither name alone is wrong,
 * and accepting the combination would price against a market the caller did not intend.
 */
public class WorldDataCenterMismatchException extends RuntimeException{
    public WorldDataCenterMismatchException(String message){
        super(message);
    }
}
