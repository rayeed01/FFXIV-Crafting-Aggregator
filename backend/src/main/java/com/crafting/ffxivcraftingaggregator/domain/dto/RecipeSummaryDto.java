package com.crafting.ffxivcraftingaggregator.domain.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * A recipe reduced to what a list view needs.
 *
 * <p>Exists so search results need not drag every ingredient of every match along with them.
 * Carries the result item's name but not its id, so callers needing the item itself must look
 * it up separately.
 */
@Builder
public record RecipeSummaryDto(UUID id,
                               int level,
                               String job,
                               String resultItemName,
                               String resultItemIconUrl) {
}
