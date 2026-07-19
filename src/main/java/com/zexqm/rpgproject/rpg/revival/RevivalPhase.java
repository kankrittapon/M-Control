package com.zexqm.rpgproject.rpg.revival;

/**
 * Dedicated player lifecycle for revival skills. This must not be inferred from combat CC state.
 */
public enum RevivalPhase {
    ALIVE,
    DOWNED,
    REVIVABLE,
    REVIVING
}
