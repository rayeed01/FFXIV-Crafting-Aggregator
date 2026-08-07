package com.crafting.ffxivcraftingaggregator;

import com.crafting.ffxivcraftingaggregator.client.UniversalisClient;
import com.crafting.ffxivcraftingaggregator.repository.WorldRepository;
import com.crafting.ffxivcraftingaggregator.service.GameServerSyncService;
import com.crafting.ffxivcraftingaggregator.service.XivapiSyncService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class FfxivCraftingAggregatorApplication {

    public static void main(String[] args) {

        SpringApplication.run(FfxivCraftingAggregatorApplication.class, args);

    }

//    @Bean
//    ApplicationRunner bootstrapWorlds(WorldRepository repo, GameServerSyncService sync) {
//        return args -> {
//            if (repo.count() == 0) {
//                try { sync.sync(); }
//                catch (Exception ex) {
//                    System.out.println("Bootstrap world sync failed");}
//            }
//        };
//    }
}
