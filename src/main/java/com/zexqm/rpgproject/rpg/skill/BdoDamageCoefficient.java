package com.zexqm.rpgproject.rpg.skill;

public record BdoDamageCoefficient(double perHit, double total, int hitCount) {
    public BdoDamageCoefficient {
        if (!Double.isFinite(perHit) || !Double.isFinite(total) || perHit < 0 || total < 0)
            throw new IllegalArgumentException("coefficients must be finite and non-negative");
        if (hitCount < 1) throw new IllegalArgumentException("hitCount must be positive");
    }
}
