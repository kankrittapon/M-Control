package com.zexqm.rpgproject.rpg.revival;

import java.util.UUID;

/**
 * Transient contract for the future downed-player lifecycle. It is intentionally not attached as a
 * capability until death interception, respawn handling, and multiplayer ownership are designed.
 */
public final class PlayerRevivalState {
    private RevivalPhase phase = RevivalPhase.ALIVE;
    private UUID downedBy;
    private long revivableUntilGameTime;

    public RevivalPhase phase() {
        return phase;
    }

    public UUID downedBy() {
        return downedBy;
    }

    public long revivableUntilGameTime() {
        return revivableUntilGameTime;
    }

    public boolean revivable(long gameTime) {
        return phase == RevivalPhase.REVIVABLE && gameTime <= revivableUntilGameTime;
    }

    public void markDowned(UUID attackerId) {
        phase = RevivalPhase.DOWNED;
        downedBy = attackerId;
        revivableUntilGameTime = 0;
    }

    public void markRevivable(long untilGameTime) {
        if (phase != RevivalPhase.DOWNED)
            throw new IllegalStateException("Only a downed player can become revivable");
        phase = RevivalPhase.REVIVABLE;
        revivableUntilGameTime = Math.max(0, untilGameTime);
    }

    public void markReviving() {
        if (phase != RevivalPhase.REVIVABLE)
            throw new IllegalStateException("Only a revivable player can begin revival");
        phase = RevivalPhase.REVIVING;
    }

    public void clear() {
        phase = RevivalPhase.ALIVE;
        downedBy = null;
        revivableUntilGameTime = 0;
    }
}
