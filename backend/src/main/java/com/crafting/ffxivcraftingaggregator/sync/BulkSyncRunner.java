package com.crafting.ffxivcraftingaggregator.sync;

import com.crafting.ffxivcraftingaggregator.client.XivapiClient;
import com.crafting.ffxivcraftingaggregator.client.dto.XivapiRecipeListResponse.RecipeRow;
import com.crafting.ffxivcraftingaggregator.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs the XIVAPI catalogue import on a background thread.
 *
 * <p>Separated from the service that starts it so the import can be transactional per page while
 * the caller returns immediately. Progress counters are passed in and updated as pages land,
 * which is what makes the status endpoint pollable.
 *
 * <p>Clears the running flag when it finishes or fails, otherwise a crashed import would block
 * every later attempt until a restart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BulkSyncRunner {

    private static final int PAGE_SIZE = 100;

    private final XivapiClient xivapiClient;
    private final RecipeSyncProcessor recipeSyncProcessor;
    private final RecipeRepository recipeRepository;


    @Async
    public void run(AtomicBoolean running, AtomicInteger syncedCount, AtomicReference<Instant> finishedAt){
        try {
            Integer after = null;

            while(true){
                List<RecipeRow> page = xivapiClient.listRecipes(PAGE_SIZE,after);
                if(page.isEmpty()){
                    break;
                }

                for(RecipeRow row : page){
                    if(row.rowId() <= 0){
                        continue;
                    }
                    try {
                        recipeSyncProcessor.syncRecipe(row);
                        syncedCount.incrementAndGet();
                    }
                    catch (Exception e){
                        log.warn("Failed to sync recipe {}: {}",row.rowId(), e.getMessage());
                    }
                }
                after = page.getLast().rowId();
            }
            recipeRepository.markResultItemsCraftable();
            log.info("Bulk sync complete. Synced {} recipes.", syncedCount.get());
        }
        catch (Exception e){
            log.error("Bulk sync failed", e);
        }
        finally {
            finishedAt.set(Instant.now());
            running.set(false);
        }
    }
}
