package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.rpg.CrowdControlType;

public record CrowdControlApplicationResult(Status status, CrowdControlType type, double points, int durationTicks) {
    public static CrowdControlApplicationResult notRequested() {
        return new CrowdControlApplicationResult(Status.NOT_REQUESTED, null, 0.0, 0);
    }

    public enum Status {
        APPLIED,
        NOT_REQUESTED,
        IFRAME,
        FRONT_GUARD,
        SUPER_ARMOR,
        GRAB_IMMUNE,
        IMMUNE,
        RESISTED,
        INVALID
    }
}
