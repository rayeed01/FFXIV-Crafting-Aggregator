package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.GameServerSyncResult;

/**
 * Import of the world and data center list from Universalis.
 *
 * <p>This is the prerequisite for everything else: world validation, the pricing scope, user
 * defaults and every client-side selector all depend on these tables being populated.
 *
 * <p>Unlike the recipe import this is quick enough to run synchronously - a few hundred rows.
 */
public interface GameServerSyncService {

    /**
     * Fetches the server list and upserts it, then refreshes the world registry's cached snapshot.
     *
     * <p>A world Universalis reports without a resolvable data center is skipped and counted
     * rather than aborting the run, since one malformed row should not block the rest.
     *
     * @return how many data centers and worlds were written, and how many worlds were skipped
     * @throws com.crafting.ffxivcraftingaggregator.exception.GameServerSyncException
     *         if Universalis cannot be reached or returns something unusable
     */
    GameServerSyncResult sync();
}
