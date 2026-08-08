package com.crafting.ffxivcraftingaggregator.exception;

public class GameServerSyncException extends RuntimeException{
    public GameServerSyncException(String message){
        super(message);
    }

    public GameServerSyncException(String message,Throwable cause){
        super(message,cause);
    }
}
