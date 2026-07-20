package com.zexqm.rpgproject.rpg.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CastTimeOverrideStateTest {
    @Test void refreshesExpiresAndClears() {
        RpgCombatState state = new RpgCombatState();
        state.activateCastTimeOverride(300);
        assertTrue(state.ignoresCastTime());
        for (int tick = 0; tick < 100; tick++) state.tick(null);
        state.activateCastTimeOverride(100);
        assertEquals(200, state.castTimeOverrideTicks());
        for (int tick = 0; tick < 200; tick++) state.tick(null);
        assertFalse(state.ignoresCastTime());
        state.activateCastTimeOverride(300);
        state.clear();
        assertEquals(0, state.castTimeOverrideTicks());
    }
}
