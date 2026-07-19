package com.zexqm.rpgproject.rpg.revival;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRevivalStateTest {
    @Test void lifecycleCannotConfuseAliveWithCombatDownedState() {
        PlayerRevivalState state = new PlayerRevivalState();
        assertEquals(RevivalPhase.ALIVE, state.phase());
        assertFalse(state.revivable(100));

        UUID attacker = UUID.randomUUID();
        state.markDowned(attacker);
        assertEquals(RevivalPhase.DOWNED, state.phase());
        assertEquals(attacker, state.downedBy());
        state.markRevivable(120);
        assertTrue(state.revivable(120));
        assertFalse(state.revivable(121));
        state.markReviving();
        state.clear();

        assertEquals(RevivalPhase.ALIVE, state.phase());
        assertNull(state.downedBy());
    }

    @Test void invalidTransitionsAreRejected() {
        PlayerRevivalState state = new PlayerRevivalState();
        assertThrows(IllegalStateException.class, () -> state.markRevivable(100));
        assertThrows(IllegalStateException.class, state::markReviving);
    }
}
