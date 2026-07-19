package com.zexqm.rpgproject.rpg.revival;

public record RevivalResult(Status status, double healthRecoveryPercent,
                            double resourceRecoveryPercent) {
    public enum Status {
        REVIVED,
        FOUNDATION_UNAVAILABLE,
        INVALID_TARGET,
        TARGET_NOT_REVIVABLE,
        OUT_OF_RANGE,
        BLOCKED_LINE_OF_SIGHT
    }
}
