package com.zexqm.rpgproject.client;

import com.zexqm.rpgproject.network.SyncEntityStatusesPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientEntityStatuses {
    private static final Map<Integer, List<SyncEntityStatusesPacket.Entry>> STATUSES = new HashMap<>();

    public static void apply(int entityId, List<SyncEntityStatusesPacket.Entry> statuses) {
        if (statuses.isEmpty()) STATUSES.remove(entityId);
        else STATUSES.put(entityId, List.copyOf(statuses));
    }

    public static List<SyncEntityStatusesPacket.Entry> get(int entityId) {
        return STATUSES.getOrDefault(entityId, List.of());
    }

    private ClientEntityStatuses() {}
}
