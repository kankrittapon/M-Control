package com.zexqm.rpgproject.rpg.combat;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RpgCombatMathTest {
    private static final CombatConfig.Values CONFIG = CombatConfig.Values.defaults();

    @Test
    void hitChanceUsesConfiguredClamp() {
        assertEquals(0.10, RpgCombatMath.hitChance(0, 100, CONFIG), 0.0001);
        assertEquals(0.50, RpgCombatMath.hitChance(100, 100, CONFIG), 0.0001);
        assertEquals(0.95, RpgCombatMath.hitChance(100, 0, CONFIG), 0.0001);
    }

    @Test
    void damageReductionUsesConfiguredCap() {
        assertEquals(20.0, RpgCombatMath.reducedDamage(100, 1.0, CONFIG), 0.0001);
        assertEquals(75.0, RpgCombatMath.reducedDamage(100, 0.25, CONFIG), 0.0001);
    }

    @Test
    void specialDamageMultipliersStack() {
        double result = RpgCombatMath.stackSpecialMultipliers(100,
                EnumSet.of(SpecialAttackType.BACK_ATTACK, SpecialAttackType.DOWN_ATTACK,
                        SpecialAttackType.AIR_ATTACK), CONFIG);
        assertEquals(244.8, result, 0.0001);
    }
}
