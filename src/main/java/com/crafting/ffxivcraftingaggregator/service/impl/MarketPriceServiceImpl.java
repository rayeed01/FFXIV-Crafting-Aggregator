package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.client.UniversalisClient;
import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketPriceServiceImpl implements MarketPriceService {

    private final StringRedisTemplate redis;
    private final UniversalisClient universalisClient;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(MarketPriceServiceImpl.class);

    private static final Duration PRICED_TTL = Duration.ofMinutes(15);
    private static final Duration UNLISTED_TTL = Duration.ofMinutes(15);
    private static final Duration UNRESOLVED_TTL = Duration.ofDays(7);

    @Override
    public UniversalisPrices getPrices(List<Integer> itemXivapiIds, String scope) {
        return null;
    }

    @Override
    public void exictScope(String scope) {

    }
}
