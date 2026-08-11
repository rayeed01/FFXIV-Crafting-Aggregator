package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.domain.dto.ItemDto;
import com.crafting.ffxivcraftingaggregator.domain.entity.Item;
import com.crafting.ffxivcraftingaggregator.exception.ItemNotFoundException;
import com.crafting.ffxivcraftingaggregator.mapper.ItemMapper;
import com.crafting.ffxivcraftingaggregator.repository.ItemRepository;
import com.crafting.ffxivcraftingaggregator.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * Item lookups over the synced catalogue.
 *
 * <p>Search is capped and blank queries short-circuit to an empty list, so a stray request cannot
 * ask the database for every row.
 */
public class ItemServiceImpl implements ItemService {

    /**
     * Rows returned by a search. Nobody scrolls past the first screen of a substring match, and
     * uncapped a one-letter query returned ~7,500 rows and 1.8 MB of JSON.
     */
    private static final int SEARCH_LIMIT = 50;

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Transactional(readOnly = true)
    @Override
    public ItemDto findItemById(UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found"));

        return itemMapper.toDto(item);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ItemDto> searchItems(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        return itemRepository
                .searchByRelevance(escapeLikeWildcards(query.trim()), PageRequest.of(0, SEARCH_LIMIT))
                .stream()
                .map(itemMapper::toDto)
                .toList();
    }

    /**
     * Neutralises the characters LIKE treats as wildcards.
     *
     * <p>Spring Data's derived Containing queries escape these automatically; an explicit LIKE
     * does not. Without this a search for "%" matches every item in the catalogue, and "_" matches
     * any single character - turning a search box into an unbounded query.
     *
     * <p>The backslash is escaped first, otherwise it would re-escape the escapes added after it.
     * The queries declare {@code ESCAPE '\'} to match.
     */
    private static String escapeLikeWildcards(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Override
    public Optional<Item> getItemByXivapiId(int xivapiId) {
        return itemRepository.findByXivapiId(xivapiId);
    }
}
