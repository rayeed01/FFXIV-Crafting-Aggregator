package com.crafting.ffxivcraftingaggregator.service;

import com.crafting.ffxivcraftingaggregator.domain.dto.GameServerSyncResult;
import com.crafting.ffxivcraftingaggregator.domain.dto.SyncStatus;

public interface GameServerSyncService {
    GameServerSyncResult sync();
}
