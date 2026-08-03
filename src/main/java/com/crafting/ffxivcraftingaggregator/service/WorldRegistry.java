package com.crafting.ffxivcraftingaggregator.service;

public interface WorldRegistry {
    String canonicalWorldName(String input);
    String canonicalDataCenterName(String input);
    void refresh();
}
