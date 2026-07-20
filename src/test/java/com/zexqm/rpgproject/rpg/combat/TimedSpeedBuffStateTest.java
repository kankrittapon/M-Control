package com.zexqm.rpgproject.rpg.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimedSpeedBuffStateTest {
    @Test void strongerBuffWinsAndEqualBuffRefreshes() {
        RpgCombatState state = new RpgCombatState();
        state.activateSpeedBuff(100, 0.15, 0.15, 0.15);
        state.activateSpeedBuff(200, 0.10, 0.10, 0.10);
        assertEquals(100, state.speedBuffTicks());
        assertEquals(0.15, state.attackSpeedBonus());

        state.activateSpeedBuff(200, 0.15, 0.15, 0.15);
        assertEquals(200, state.speedBuffTicks());
        for (int tick = 0; tick < 200; tick++) state.tick(null);
        assertEquals(0, state.speedBuffTicks());
        assertEquals(0.0, state.attackSpeedBonus());
        assertEquals(0.0, state.castingSpeedBonus());
        assertEquals(0.0, state.timedMovementSpeedBonus());
    }
}
