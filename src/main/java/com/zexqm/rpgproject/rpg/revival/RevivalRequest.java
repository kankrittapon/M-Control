package com.zexqm.rpgproject.rpg.revival;

import java.util.UUID;

public record RevivalRequest(UUID casterId, UUID targetId, double range,
                             double healthRecoveryPercent, double resourceRecoveryPercent) {
    public RevivalRequest {
        if (casterId == null || targetId == null || !Double.isFinite(range) || range <= 0
                || !validPercent(healthRecoveryPercent) || !validPercent(resourceRecoveryPercent))
            throw new IllegalArgumentException("Invalid revival request");
    }

    private static boolean validPercent(double value) {
        return Double.isFinite(value) && value >= 0 && value <= 1;
    }
}
