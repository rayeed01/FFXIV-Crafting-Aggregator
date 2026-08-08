package com.crafting.ffxivcraftingaggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * <p>Configuration comes from the root {@code .env} via {@code spring.config.import}, so the app
 * starts the same way from Maven, an IDE or a jar without variables being exported first. Real
 * environment variables still take precedence, which is what a deployment supplies.
 */
@SpringBootApplication
public class FfxivCraftingAggregatorApplication {

    public static void main(String[] args) {

        SpringApplication.run(FfxivCraftingAggregatorApplication.class, args);

    }
}
