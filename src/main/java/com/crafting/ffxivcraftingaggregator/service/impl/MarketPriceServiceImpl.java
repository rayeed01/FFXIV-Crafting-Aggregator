package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.client.UniversalisClient;
import com.crafting.ffxivcraftingaggregator.client.dto.ItemPrice;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.domain.dto.CachedPrice;
import com.crafting.ffxivcraftingaggregator.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MarketPriceServiceImpl implements MarketPriceService {

    private static final Logger log = LoggerFactory.getLogger(MarketPriceServiceImpl.class);
    private static final Duration PRICED_TTL = Duration.ofMinutes(15);
    private static final Duration UNLISTED_TTL = Duration.ofMinutes(15);
    private static final Duration UNRESOLVED_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final UniversalisClient universalisClient;
    private final ObjectMapper objectMapper;

    @Override
    public Map<Integer, CachedPrice> getPrices(List<Integer> itemXivapiIds, String scope) {
        if (itemXivapiIds == null || itemXivapiIds.isEmpty()) {
            return Map.of();
        }

        List<Integer> distinctIds = itemXivapiIds.stream().distinct().toList();

        Map<Integer, CachedPrice> result = new HashMap<>();
        List<Integer> misses = readFromCache(distinctIds, scope, result);

        if (misses.isEmpty()) {
            log.debug("Price lookup for scope {}: {} ids, all served from cache", scope, distinctIds.size());
            return result;
        }
        UniversalisPrices fetched = universalisClient.getPrices(misses, scope);

        Map<Integer, CachedPrice> toCache = new HashMap<>();

        for (Integer id : misses) {
            CachedPrice cached = toCachedPrice(id, fetched);
            toCache.put(id, cached);
            result.put(id, cached);
        }

        writeToCache(toCache, scope);

        log.debug("Price lookup for scope {}: {} ids, {} cache hits, {} fetched",
                scope, distinctIds.size(), distinctIds.size() - misses.size(), misses.size());

        return result;
    }

    @Override
    public void evictScope(String scope) {
        Set<String> keys = redis.keys(keyPrefix(scope) + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
            log.info("Evicted {} cached prices for scope {}", keys.size(), scope);
        }
    }

    private List<Integer> readFromCache(List<Integer> ids, String scope, Map<Integer, CachedPrice> result) {

        List<String> keys = ids.stream().map(id -> key(scope, id)).toList();

        List<String> raw;
        try {
            raw = redis.opsForValue().multiGet(keys);
        } catch (RuntimeException ex) {
            log.warn("Redis read failed for scope {}; falling back to a direct fetch", scope, ex);
            return new ArrayList<>(ids);
        }

        if (raw == null) {
            return new ArrayList<>(ids);
        }

        List<Integer> misses = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            String json = raw.get(i);
            Integer id = ids.get(i);

            if (json == null) {
                misses.add(id);
                continue;
            }

            CachedPrice cached = deserialize(json);
            if (cached == null) {
                misses.add(id);
            } else {
                result.put(id, cached);
            }
        }

        return misses;
    }

    private void writeToCache(Map<Integer, CachedPrice> entries, String scope) {
        try {
            entries.forEach((id, price) ->
                    redis.opsForValue().set(key(scope, id), serialise(price), ttlFor(price.status())));
        } catch (RuntimeException ex) {
            log.warn("Redis write failed for scope {}", scope, ex);
        }
    }

    private static Duration ttlFor(CachedPrice.Status status) {
        return switch (status) {
            case PRICED -> PRICED_TTL;
            case UNLISTED -> UNLISTED_TTL;
            case UNRESOLVED -> UNRESOLVED_TTL;
        };
    }

    private CachedPrice toCachedPrice(Integer id, UniversalisPrices fetched) {
        if (fetched.isUnresolved(id)) {
            return CachedPrice.unresolved();
        }

        ItemPrice price = fetched.prices().get(id);
        if (price == null) {
            log.warn("Item {} was neither priced nor unresolved; caching as unresolved", id);
            return CachedPrice.unresolved();
        }

        if (!price.hasListing()) {
            return CachedPrice.unlisted();
        }

        return CachedPrice.priced(price);
    }

    private static String key(String scope, int itemXivapiId) {
        return keyPrefix(scope) + itemXivapiId;
    }

    private static String keyPrefix(String scope) {
        return "price:" + scope + ":";
    }

    private String serialise(CachedPrice price) {
        return objectMapper.writeValueAsString(price);
    }

    private CachedPrice deserialize(String json) {
        try {
            return objectMapper.readValue(json, CachedPrice.class);
        } catch (RuntimeException ex) {
            log.warn("Discarding unreadable cache entry", ex);
            return null;
        }
    }
}