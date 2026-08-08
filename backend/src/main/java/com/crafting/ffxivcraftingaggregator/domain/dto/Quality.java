package com.crafting.ffxivcraftingaggregator.domain.dto;

/**
 * Which market listing to price against.
 *
 * <p>{@link #CHEAPEST} is the default and preserves the original behaviour: whichever of NQ or HQ
 * is currently listed lower.
 */
public enum Quality {
    CHEAPEST,
    NQ,
    HQ
}
