package com.zexqm.rpgproject.rpg.combat;

public record ProtectionDecision(Reason reason, boolean blockDamage, boolean blockCrowdControl,
                                 boolean blockKnockback, boolean consumeGuard) {
    public enum Reason { NONE, IFRAME, FRONT_GUARD, SUPER_ARMOR, PERFECT_GUARD }

    public static final ProtectionDecision NONE = new ProtectionDecision(Reason.NONE,
            false, false, false, false);
}
