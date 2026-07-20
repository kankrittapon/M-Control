package com.zexqm.rpgproject.rpg.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimedDamageReductionServiceTest {
    @Test void halvesFinalIncomingDamage() {
        assertEquals(5.0F, TimedDamageReductionService.reduce(10.0F, 0.50), 0.0001F);
        assertEquals(10.0F, TimedDamageReductionService.reduce(10.0F, 0.0), 0.0001F);
    }

    @Test void strongerBuffReplacesWeakerAndWeakerDoesNotExtendIt() {
        var state = new RpgCombatState();
        state.activateDamageReductionBuff(100, 0.25);
        state.activateDamageReductionBuff(40, 0.50);
        state.activateDamageReductionBuff(200, 0.20);
        assertEquals(0.50, state.damageReductionBuff());
        assertEquals(40, state.damageReductionBuffTicks());
    }

    @Test void equalBuffKeepsLongerDuration() {
        var state = new RpgCombatState();
        state.activateDamageReductionBuff(80, 0.50);
        state.activateDamageReductionBuff(120, 0.50);
        assertEquals(120, state.damageReductionBuffTicks());
    }
}
