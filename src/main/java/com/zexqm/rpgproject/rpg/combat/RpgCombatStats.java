package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.registry.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;

public record RpgCombatStats(double attackPower, double magicPower, double accuracy,
                             double evasion, double damageReduction,
                             double criticalChance, double criticalDamage, double ccResistance) {
    public static RpgCombatStats resolve(LivingEntity entity) {
        double defenseDown = entity.getCapability(RpgCombatStateProvider.DATA)
                .map(state -> state.statusPotency(RpgStatusType.DEFENSE_DOWN)).orElse(0.0);
        return new RpgCombatStats(value(entity, ModAttributes.ATTACK_POWER.get(), 0.0),
                value(entity, ModAttributes.MAGIC_POWER.get(), 0.0),
                value(entity, ModAttributes.ACCURACY.get(), 100.0),
                value(entity, ModAttributes.EVASION.get(), 0.0),
                Math.max(0.0, value(entity, ModAttributes.DAMAGE_REDUCTION.get(), 0.0) - defenseDown),
                value(entity, ModAttributes.CRITICAL_CHANCE.get(), 0.0),
                value(entity, ModAttributes.CRITICAL_DAMAGE.get(), 2.0),
                value(entity, ModAttributes.CC_RESISTANCE.get(), 0.0));
    }

    private static double value(LivingEntity entity, Attribute attribute, double fallback) {
        return entity.getAttribute(attribute) == null ? fallback : entity.getAttributeValue(attribute);
    }
}
