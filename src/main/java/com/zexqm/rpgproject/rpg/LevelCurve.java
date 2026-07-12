package com.zexqm.rpgproject.rpg;

public final class LevelCurve {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 100;

    public static long requiredExp(int level) {
        if (level >= MAX_LEVEL) return 0;
        long l = Math.max(MIN_LEVEL, level);
        return 100L + 35L * l * l + 65L * l;
    }

    private LevelCurve() {}
}
