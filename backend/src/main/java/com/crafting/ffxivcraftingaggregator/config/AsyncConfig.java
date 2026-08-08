package com.crafting.ffxivcraftingaggregator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Thread pool for background work.
 *
 * <p>Exists so the catalogue import does not run on a request thread, where it would hold a
 * connection for the length of the sync.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
