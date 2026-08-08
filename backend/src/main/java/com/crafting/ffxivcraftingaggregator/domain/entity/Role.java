package com.crafting.ffxivcraftingaggregator.domain.entity;

/**
 * Account roles.
 *
 * <p>Spring Security sees these prefixed as {@code ROLE_USER} and {@code ROLE_ADMIN}. New accounts
 * are always {@code USER}; promotion is a manual database change on purpose.
 */
public enum Role {
    USER,
    ADMIN
}
