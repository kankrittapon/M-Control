package com.zexqm.rpgproject.rpg.skill;

public final class BdoDamageFormula {
    private BdoDamageFormula() {}

    public static BdoDamageCoefficient convert(double damagePercent, int hitCount) {
        return convert(damagePercent, hitCount, SkillRuntimeConfig.values().bdoDamage());
    }

    public static BdoDamageCoefficient convert(double damagePercent, int hitCount,
                                                SkillRuntimeConfig.BdoDamageValues config) {
        if (!Double.isFinite(damagePercent) || damagePercent <= 0)
            throw new IllegalArgumentException("damagePercent must be finite and positive");
        if (hitCount < 1 || hitCount > config.maxSourceHits())
            throw new IllegalArgumentException("hitCount is outside the configured source range");

        double normalized = damagePercent / config.referencePercent();
        double perHit = config.multiplier() * Math.pow(normalized, config.exponent());
        perHit = clamp(perHit, config.minPerHitCoefficient(), config.maxPerHitCoefficient());
        double total = Math.min(perHit * hitCount, config.maxTotalCoefficient());
        return new BdoDamageCoefficient(perHit, total, hitCount);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
