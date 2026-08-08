package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.DataCenterDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.WorldDto;

import java.util.List;

public interface WorldService {
    List<WorldDto> getAllWorlds();
    List<DataCenterDto> getAllDataCenters();
}
