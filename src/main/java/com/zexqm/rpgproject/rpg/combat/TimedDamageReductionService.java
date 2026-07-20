package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.world.entity.LivingEntity;

public final class TimedDamageReductionService {
    public static float reduce(LivingEntity target, float incomingDamage) {
        if (incomingDamage <= 0) return 0;
        double ratio = target.getCapability(RpgCombatStateProvider.DATA)
                .map(RpgCombatState::damageReductionBuff).orElse(0.0);
        float remaining = reduce(incomingDamage, ratio);
        if (ratio > 0) RpgProject.LOGGER.info(
                "[RPG Timed DR] target={} incoming={} ratio={} remaining={}",
                target.getScoreboardName(), incomingDamage, ratio, remaining);
        return remaining;
    }

    static float reduce(float incomingDamage, double ratio) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        return (float) (Math.max(0.0F, incomingDamage) * (1.0 - clamped));
    }

    private TimedDamageReductionService() {}
}
