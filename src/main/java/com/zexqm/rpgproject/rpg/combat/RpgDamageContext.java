package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.rpg.CrowdControlType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record RpgDamageContext(LivingEntity attacker, LivingEntity target, Vec3 origin,
                               double baseDamage, RpgPowerType powerType, double coefficient,
                               boolean accuracyEligible, boolean criticalEligible,
                               double hitChanceBonus, double criticalChanceBonus,
                               CrowdControlType crowdControl,
                               Set<SpecialAttackType> specialAttackEligibility) {
    public RpgDamageContext {
        if (attacker == null || target == null) throw new IllegalArgumentException("attacker and target are required");
        if (baseDamage < 0 || coefficient < 0 || hitChanceBonus < 0 || hitChanceBonus > 1
                || criticalChanceBonus < 0 || criticalChanceBonus > 1)
            throw new IllegalArgumentException("Invalid damage values or hit modifiers");
        powerType = powerType == null ? RpgPowerType.NONE : powerType;
        specialAttackEligibility = specialAttackEligibility == null || specialAttackEligibility.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(specialAttackEligibility));
    }

    public RpgDamageContext(LivingEntity attacker, LivingEntity target, Vec3 origin,
                            double baseDamage, RpgPowerType powerType, double coefficient,
                            boolean accuracyEligible, boolean criticalEligible,
                            CrowdControlType crowdControl, Set<SpecialAttackType> specialAttackEligibility) {
        this(attacker, target, origin, baseDamage, powerType, coefficient, accuracyEligible,
                criticalEligible, 0.0, 0.0, crowdControl, specialAttackEligibility);
    }

    public static RpgDamageContext basic(LivingEntity attacker, LivingEntity target, double baseDamage) {
        return new RpgDamageContext(attacker, target, attacker.position(), baseDamage, RpgPowerType.ATTACK,
                1.0, true, true, 0.0, 0.0, null, EnumSet.allOf(SpecialAttackType.class));
    }
}
