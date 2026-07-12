package com.zexqm.rpgproject.rpg;

public record ClassGrowthProfile(
        double healthBase, double healthPerLevel, double manaBase, double manaPerLevel,
        double attackBase, double attackPerLevel, double magicBase, double magicPerLevel,
        double defenseBase, double defensePerLevel, double accuracyBase, double accuracyPerLevel,
        double evasionBase, double evasionPerLevel, double attackSpeed, double castSpeed,
        double moveSpeed, double ccResistance) {

    public static ClassGrowthProfile defaults(RpgClass cls) {
        return switch (cls) {
            case WIZARD -> new ClassGrowthProfile(90, 7, 140, 12, 8, 1.2, 18, 3.2,
                    7, 1.1, 100, 2.0, 45, 1.0, 1.0, 1.12, 1.0, 0);
            case NINJA -> new ClassGrowthProfile(105, 8, 85, 6, 16, 2.8, 7, 1.0,
                    8, 1.2, 105, 2.2, 70, 1.8, 1.14, 1.0, 1.08, 0);
        };
    }

    public DerivedStats atLevel(int level) {
        int gained = Math.max(0, level - 1);
        double defense = defenseBase + defensePerLevel * gained;
        double dr = Math.min(0.80, defense / (defense + 100.0 + level * 5.0));
        return new DerivedStats(healthBase + healthPerLevel * gained, manaBase + manaPerLevel * gained,
                attackBase + attackPerLevel * gained, magicBase + magicPerLevel * gained,
                defense, dr, accuracyBase + accuracyPerLevel * gained,
                evasionBase + evasionPerLevel * gained, 0.05, 1.50,
                attackSpeed, castSpeed, moveSpeed, ccResistance);
    }
}
