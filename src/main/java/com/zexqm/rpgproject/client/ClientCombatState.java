package com.zexqm.rpgproject.client;

import com.zexqm.rpgproject.network.SyncCombatStatePacket;
import com.zexqm.rpgproject.rpg.CrowdControlType;

public final class ClientCombatState {
    private static double guard;
    private static double maximumGuard;
    private static double ccPoints;
    private static int immunityTicks;
    private static CrowdControlType activeCc;
    private static int activeTicks;
    private static boolean casting;

    public static void apply(SyncCombatStatePacket packet) {
        guard = packet.guard();
        maximumGuard = packet.maximumGuard();
        ccPoints = packet.ccPoints();
        immunityTicks = packet.immunityTicks();
        activeCc = packet.activeCc();
        activeTicks = packet.activeTicks();
        casting = packet.casting();
    }

    public static void tick() {
        if (immunityTicks > 0) immunityTicks--;
        if (activeTicks > 0) activeTicks--;
        if (activeTicks == 0 && activeCc != CrowdControlType.FLOAT) activeCc = null;
    }

    public static boolean actionLocked() {
        return activeCc != null && (activeTicks > 0 || activeCc == CrowdControlType.FLOAT);
    }

    public static double guard() { return guard; }
    public static double maximumGuard() { return maximumGuard; }
    public static double ccPoints() { return ccPoints; }
    public static int immunityTicks() { return immunityTicks; }
    public static CrowdControlType activeCc() { return activeCc; }
    public static boolean casting() { return casting; }

    private ClientCombatState() {}
}
