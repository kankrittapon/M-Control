package com.zexqm.rpgproject.rpg.combat;

public final class ProtectionResolver {
    public static ProtectionDecision resolveDamage(RpgCombatState state, CombatImpactContext impact) {
        if (impact.bypassesInvulnerability()) return ProtectionDecision.NONE;
        if (state.iframe()) return decision(ProtectionDecision.Reason.IFRAME, true, true, true, false);

        boolean front = state.frontGuard() && impact.hasDirectionalOrigin()
                && RpgCombatMath.withinFacingArc(impact.target(), impact.origin(),
                CombatConfig.values().frontalGuardArcDegrees());
        if (front) return decision(state.superArmor() ? ProtectionDecision.Reason.PERFECT_GUARD
                        : ProtectionDecision.Reason.FRONT_GUARD,
                true, true, true, true);
        if (state.superArmor()) return decision(ProtectionDecision.Reason.SUPER_ARMOR,
                false, true, true, false);
        return ProtectionDecision.NONE;
    }

    public static ProtectionDecision resolveKnockback(RpgCombatState state, CombatImpactContext impact) {
        if (state.iframe()) return decision(ProtectionDecision.Reason.IFRAME, true, true, true, false);
        boolean front = state.frontGuard() && impact.hasDirectionalOrigin()
                && RpgCombatMath.withinFacingArc(impact.target(), impact.origin(),
                CombatConfig.values().frontalGuardArcDegrees());
        if (front) return decision(state.superArmor() ? ProtectionDecision.Reason.PERFECT_GUARD
                        : ProtectionDecision.Reason.FRONT_GUARD,
                true, true, true, false);
        if (state.superArmor()) return decision(ProtectionDecision.Reason.SUPER_ARMOR,
                false, true, true, false);
        return ProtectionDecision.NONE;
    }

    public static boolean perfectGuard(RpgCombatState state) {
        return state.frontGuard() && state.superArmor();
    }

    private static ProtectionDecision decision(ProtectionDecision.Reason reason, boolean damage,
                                                 boolean cc, boolean knockback, boolean guard) {
        return new ProtectionDecision(reason, damage, cc, knockback, guard);
    }

    private ProtectionResolver() {}
}
