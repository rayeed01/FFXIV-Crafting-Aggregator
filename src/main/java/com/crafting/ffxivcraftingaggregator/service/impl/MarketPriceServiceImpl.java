package com.crafting.ffxivcraftingaggregator.service.impl;

import com.crafting.ffxivcraftingaggregator.client.dto.UniversalisPrices;
import com.crafting.ffxivcraftingaggregator.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkerPriceServiceImpl implements MarketPriceService {



    @Override
    public UniversalisPrices getPrices(List<Integer> itemXivapiIds, String scope) {
        return null;
    }

    @Override
    public void exictScope(String scope) {

    }
}
