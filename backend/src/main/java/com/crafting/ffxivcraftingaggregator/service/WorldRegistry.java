package com.crafting.ffxivcraftingaggregator.service;

public interface WorldRegistry {
    String canonicalWorldName(String input);
    String canonicalDataCenterName(String input);
    void validateWorldBelongsToDataCenter(String worldName, String dataCenterName);
    String dataCenterForWorld(String worldName);
    void refresh();

}
