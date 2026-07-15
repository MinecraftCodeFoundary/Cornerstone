package org.yeyao.cornerstone.teleport;

import org.yeyao.cornerstone.data.StoredLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persisted M2 state. Live requests and scheduled teleports are intentionally not persisted. */
final class TeleportData {
    StoredLocation globalSpawn;
    final Map<String, StoredLocation> dimensionSpawns = new LinkedHashMap<>();
    final Map<UUID, Map<String, StoredLocation>> homes = new LinkedHashMap<>();
    final Map<UUID, StoredLocation> backLocations = new LinkedHashMap<>();
    final Map<String, Warp> warps = new LinkedHashMap<>();
}
