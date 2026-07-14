package com.zexqm.rpgproject.rpg.mob;

public enum MobControlProfile {
    NORMAL(1.0, 0.0, false, false),
    ELITE(0.6, 0.25, false, false),
    BOSS(0.5, 1.0, true, false),
    UNSTOPPABLE(0.0, 1.0, true, true),
    PLAYER(1.0, 0.0, false, false);

    private final double durationMultiplier;
    private final double resistanceBonus;
    private final boolean hardCcImmune;
    private final boolean statusImmune;

    MobControlProfile(double durationMultiplier, double resistanceBonus, boolean hardCcImmune, boolean statusImmune) {
        this.durationMultiplier = durationMultiplier;
        this.resistanceBonus = resistanceBonus;
        this.hardCcImmune = hardCcImmune;
        this.statusImmune = statusImmune;
    }

    public double durationMultiplier() { return durationMultiplier; }
    public double resistanceBonus() { return resistanceBonus; }
    public boolean hardCcImmune() { return hardCcImmune; }
    public boolean statusImmune() { return statusImmune; }
}
