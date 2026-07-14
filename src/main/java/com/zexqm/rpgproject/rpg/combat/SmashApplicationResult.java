package com.zexqm.rpgproject.rpg.combat;

public record SmashApplicationResult(Status status, SmashType type, int extensionTicks) {
    public static SmashApplicationResult notRequested() {
        return new SmashApplicationResult(Status.NOT_REQUESTED, null, 0);
    }

    public enum Status {
        APPLIED,
        NOT_REQUESTED,
        WRONG_STATE,
        CHANCE_FAILED,
        IFRAME,
        PROFILE_IMMUNE,
        INVALID
    }
}
