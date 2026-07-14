package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import com.zexqm.rpgproject.rpg.mob.MobControlProfiles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SmashResolver {
    public static SmashApplicationResult apply(LivingEntity target, SmashType type, double chance, Vec3 origin) {
        if (target == null || type == null || chance < 0.0 || chance > 1.0)
            return result(SmashApplicationResult.Status.INVALID, type, 0);
        RpgCombatState state = target.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (state == null) return result(SmashApplicationResult.Status.INVALID, type, 0);
        if (state.iframe()) return result(SmashApplicationResult.Status.IFRAME, type, 0);
        MobControlProfile profile = MobControlProfiles.resolve(target);
        if (profile == MobControlProfile.BOSS || profile == MobControlProfile.UNSTOPPABLE)
            return result(SmashApplicationResult.Status.PROFILE_IMMUNE, type, 0);
        boolean eligible = type == SmashType.DOWN_SMASH ? state.downed() : state.floated();
        if (!eligible) return result(SmashApplicationResult.Status.WRONG_STATE, type, 0);
        if (target.getRandom().nextDouble() >= chance)
            return result(SmashApplicationResult.Status.CHANCE_FAILED, type, 0);

        CombatConfig.Values config = CombatConfig.values();
        int extension = type == SmashType.DOWN_SMASH
                ? config.downSmashExtensionTicks() : config.airSmashExtensionTicks();
        state.applySmash(type, extension);
        state.applyImpulse(target, origin, config.smashHorizontalVelocity(),
                type == SmashType.AIR_SMASH ? config.airSmashVerticalVelocity() : 0.1);
        return result(SmashApplicationResult.Status.APPLIED, type, extension);
    }

    private static SmashApplicationResult result(SmashApplicationResult.Status status, SmashType type, int ticks) {
        return new SmashApplicationResult(status, type, ticks);
    }

    private SmashResolver() {}
}
