package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.rpg.CrowdControlType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import com.zexqm.rpgproject.rpg.mob.MobControlProfiles;

public final class CrowdControlResolver {
    public static CrowdControlApplicationResult apply(LivingEntity target, CrowdControlType type, Vec3 origin) {
        if (type == null) return CrowdControlApplicationResult.notRequested();
        RpgCombatState state = target.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (state == null) return result(CrowdControlApplicationResult.Status.INVALID, type, 0.0, 0);
        if (state.iframe()) return result(CrowdControlApplicationResult.Status.IFRAME, type, 0.0, 0);
        if (type == CrowdControlType.GRAB) {
            return state.grabImmune()
                    ? result(CrowdControlApplicationResult.Status.GRAB_IMMUNE, type, 0.0, 0)
                    : result(CrowdControlApplicationResult.Status.APPLIED, type, 0.0, 0);
        }
        if (state.frontGuard() && RpgCombatMath.withinFacingArc(target, origin,
                CombatConfig.values().frontalGuardArcDegrees())) {
            return result(CrowdControlApplicationResult.Status.FRONT_GUARD, type, 0.0, 0);
        }
        if (state.superArmor()) return result(CrowdControlApplicationResult.Status.SUPER_ARMOR, type, 0.0, 0);
        if (state.ccImmunityTicks() > 0) return result(CrowdControlApplicationResult.Status.IMMUNE, type, 0.0, 0);

        MobControlProfile profile = MobControlProfiles.resolve(target);
        if (profile.hardCcImmune()) return result(CrowdControlApplicationResult.Status.IMMUNE, type, 0.0, 0);

        double resistance = Math.max(0.0, Math.min(1.0,
                RpgCombatStats.resolve(target).ccResistance() + profile.resistanceBonus()));
        if (target.getRandom().nextDouble() < resistance) {
            return result(CrowdControlApplicationResult.Status.RESISTED, type, 0.0, 0);
        }

        CombatConfig.Values config = CombatConfig.values();
        double points = type == CrowdControlType.STIFFNESS ? config.stiffnessPoints() : config.standardCcPoints();
        int ticks = Math.max(1, (int) Math.round(duration(type, config) * profile.durationMultiplier()));
        state.applyCc(type, ticks, points);
        if (type == CrowdControlType.FLOAT) {
            Vec3 velocity = target.getDeltaMovement();
            target.setDeltaMovement(velocity.x, config.floatingVelocity(), velocity.z);
            target.hurtMarked = true;
        } else if (type == CrowdControlType.KNOCKBACK) {
            state.applyImpulse(target, origin);
        }
        return result(CrowdControlApplicationResult.Status.APPLIED, type, points, ticks);
    }

    private static int duration(CrowdControlType type, CombatConfig.Values config) {
        return switch (type) {
            case STIFFNESS -> config.stiffnessTicks();
            case STUN -> config.stunTicks();
            case KNOCKDOWN -> config.knockdownTicks();
            case FLOAT -> config.floatingTicks();
            case BOUND -> config.boundTicks();
            case KNOCKBACK -> config.knockbackTicks();
            case FREEZE -> config.freezingTicks();
            case GRAB -> 0;
        };
    }

    private static CrowdControlApplicationResult result(CrowdControlApplicationResult.Status status,
                                                         CrowdControlType type, double points, int ticks) {
        return new CrowdControlApplicationResult(status, type, points, ticks);
    }

    private CrowdControlResolver() {}
}
