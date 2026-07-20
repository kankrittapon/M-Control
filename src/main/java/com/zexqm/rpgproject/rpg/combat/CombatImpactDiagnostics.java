package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class CombatImpactDiagnostics {
    public static Snapshot inspect(RpgCombatState state, CombatImpactContext impact) {
        ProtectionDecision damage = ProtectionResolver.resolveDamage(state, impact);
        ProtectionDecision knockback = ProtectionResolver.resolveKnockback(state, impact);
        return new Snapshot(impact.category(), impact.origin(), facingAngle(impact), state.frontGuard(),
                state.superArmor(), state.iframe(), ProtectionResolver.perfectGuard(state),
                impact.incomingDamage(), damage.blockDamage() ? 0.0F : impact.incomingDamage(),
                state.guard(), state.guard(), damage, knockback);
    }

    public static Snapshot probe(RpgCombatState state, CombatImpactContext impact) {
        Snapshot inspected = inspect(state, impact);
        double guardBefore = state.guard();
        boolean blocked = inspected.damageDecision().blockDamage()
                && (!inspected.damageDecision().consumeGuard() || state.absorbGuard(impact.incomingDamage()));
        return new Snapshot(impact.category(), impact.origin(), inspected.facingAngleDegrees(),
                inspected.frontGuard(), inspected.superArmor(), inspected.iframe(), inspected.perfectGuard(),
                impact.incomingDamage(), blocked ? 0.0F : impact.incomingDamage(), guardBefore, state.guard(),
                inspected.damageDecision(), inspected.knockbackDecision());
    }

    public static void log(String operation, String target, Snapshot snapshot) {
        if (!CombatConfig.values().logImpactDecisions()) return;
        RpgProject.LOGGER.info("[RPG Impact Debug] operation={} target={} category={} origin={} angle={} "
                        + "FG={} SA={} IF={} PG={} damage={}->{} guard={}->{} protection={} knockbackBlocked={}",
                operation, target, snapshot.category(), snapshot.origin(), snapshot.facingAngleDegrees(),
                snapshot.frontGuard(), snapshot.superArmor(), snapshot.iframe(), snapshot.perfectGuard(),
                snapshot.damageBefore(), snapshot.damageAfter(), snapshot.guardBefore(), snapshot.guardAfter(),
                snapshot.damageDecision().reason(), snapshot.knockbackDecision().blockKnockback());
    }

    private static double facingAngle(CombatImpactContext impact) {
        if (!impact.hasDirectionalOrigin()) return Double.NaN;
        Vec3 forward = impact.target().getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 towardOrigin = impact.origin().subtract(impact.target().position()).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() <= 1.0E-8 || towardOrigin.lengthSqr() <= 1.0E-8) return Double.NaN;
        return Math.toDegrees(Math.acos(Mth.clamp(forward.dot(towardOrigin), -1.0, 1.0)));
    }

    public record Snapshot(CombatImpactCategory category, Vec3 origin, double facingAngleDegrees,
                           boolean frontGuard, boolean superArmor, boolean iframe, boolean perfectGuard,
                           float damageBefore, float damageAfter, double guardBefore, double guardAfter,
                           ProtectionDecision damageDecision, ProtectionDecision knockbackDecision) {}

    private CombatImpactDiagnostics() {}
}
