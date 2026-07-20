package com.zexqm.rpgproject.rpg.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SustainedResourceStateTest {
    @Test void pulsesRefreshesAndExpiresWithoutPersistence() {
        RpgCombatState state = new RpgCombatState();
        state.activateSustainedResource(400, 200, 50, 0.10);
        for (int tick = 0; tick < 199; tick++) state.tick(null);
        assertEquals(0, state.consumePendingMpRecovery());
        state.tick(null);
        assertEquals(50, state.consumePendingMpRecovery());
        assertEquals(0.10, state.movementSpeedBonus());

        state.activateSustainedResource(200, 100, 75, 0.15);
        for (int tick = 0; tick < 100; tick++) state.tick(null);
        assertEquals(75, state.consumePendingMpRecovery());
        for (int tick = 0; tick < 100; tick++) state.tick(null);
        assertEquals(75, state.consumePendingMpRecovery());
        assertEquals(0.0, state.movementSpeedBonus());
        assertEquals(0, state.sustainedResourceTicks());
    }
}
