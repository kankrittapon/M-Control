package com.zexqm.rpgproject.rpg;

public final class ProtectionState {
    private int frontGuardTicks, superArmorTicks, iframeTicks, grabImmuneTicks;
    private double guard = 100.0;
    private double maxGuard = 100.0;

    public boolean frontGuard() { return frontGuardTicks > 0 && guard > 0; }
    public boolean superArmor() { return superArmorTicks > 0; }
    public boolean iframe() { return iframeTicks > 0; }
    public boolean grabImmune() { return grabImmuneTicks > 0 || iframe(); }
    public double guard() { return guard; }
    public double maxGuard() { return maxGuard; }

    public void activateFrontGuard(int ticks) { frontGuardTicks = Math.max(frontGuardTicks, ticks); }
    public void activateSuperArmor(int ticks) { superArmorTicks = Math.max(superArmorTicks, ticks); }
    public void activateIframe(int ticks) { iframeTicks = Math.max(iframeTicks, ticks); }
    public void activateGrabImmunity(int ticks) { grabImmuneTicks = Math.max(grabImmuneTicks, ticks); }

    public boolean absorbGuard(double damage) {
        if (!frontGuard()) return false;
        guard = Math.max(0, guard - Math.max(1.0, damage));
        if (guard == 0) frontGuardTicks = 0;
        return true;
    }

    public void tick(boolean inCombat) {
        if (frontGuardTicks > 0) frontGuardTicks--;
        if (superArmorTicks > 0) superArmorTicks--;
        if (iframeTicks > 0) iframeTicks--;
        if (grabImmuneTicks > 0) grabImmuneTicks--;
        if (!inCombat && frontGuardTicks == 0) guard = Math.min(maxGuard, guard + 0.5);
    }

    public void clear() { frontGuardTicks = superArmorTicks = iframeTicks = grabImmuneTicks = 0; }
}
