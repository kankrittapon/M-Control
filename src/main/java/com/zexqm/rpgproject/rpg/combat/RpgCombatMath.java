package com.zexqm.rpgproject.rpg.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class RpgCombatMath {
    public static double hitChance(double accuracy, double evasion, CombatConfig.Values config) {
        return Mth.clamp(accuracy / Math.max(1.0, accuracy + evasion),
                config.minimumHitChance(), config.maximumHitChance());
    }

    public static double reducedDamage(double damage, double reduction, CombatConfig.Values config) {
        return Math.max(0.0, damage * (1.0 - Mth.clamp(reduction, 0.0, config.maximumDamageReduction())));
    }

    public static boolean withinFacingArc(LivingEntity target, Vec3 origin, double arcDegrees) {
        if (origin == null) return false;
        Vec3 forward = target.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 towardOrigin = origin.subtract(target.position()).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() <= 1.0E-6 || towardOrigin.lengthSqr() <= 1.0E-6) return false;
        double threshold = Math.cos(Math.toRadians(arcDegrees / 2.0));
        return forward.dot(towardOrigin) >= threshold;
    }

    public static boolean withinRearArc(LivingEntity target, Vec3 origin, double arcDegrees) {
        if (origin == null) return false;
        Vec3 forward = target.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 towardOrigin = origin.subtract(target.position()).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() <= 1.0E-6 || towardOrigin.lengthSqr() <= 1.0E-6) return false;
        double threshold = Math.cos(Math.toRadians(arcDegrees / 2.0));
        return forward.scale(-1.0).dot(towardOrigin) >= threshold;
    }

    public static double stackSpecialMultipliers(double damage, Set<SpecialAttackType> types,
                                                 CombatConfig.Values config) {
        double result = damage;
        for (SpecialAttackType type : types) {
            result *= switch (type) {
                case BACK_ATTACK -> config.backAttackMultiplier();
                case DOWN_ATTACK -> config.downAttackMultiplier();
                case AIR_ATTACK -> config.airAttackMultiplier();
                case SPEED_ATTACK -> config.speedAttackMultiplier();
                case COUNTER_ATTACK -> config.counterAttackMultiplier();
            };
        }
        return result;
    }

    private RpgCombatMath() {}
}
