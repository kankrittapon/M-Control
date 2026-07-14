package com.zexqm.rpgproject.rpg.combat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record RpgDamageResult(Outcome outcome, double damage, boolean critical,
                              Set<SpecialAttackType> specialAttacks,
                              CrowdControlApplicationResult crowdControl) {
    public RpgDamageResult {
        specialAttacks = specialAttacks == null || specialAttacks.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(specialAttacks));
    }

    public static RpgDamageResult stopped(Outcome outcome) {
        return new RpgDamageResult(outcome, 0.0, false, Collections.emptySet(), CrowdControlApplicationResult.notRequested());
    }

    public enum Outcome {
        HIT,
        MISS,
        IFRAME,
        FROZEN_IMMUNE,
        GUARDED,
        INVALID
    }
}
