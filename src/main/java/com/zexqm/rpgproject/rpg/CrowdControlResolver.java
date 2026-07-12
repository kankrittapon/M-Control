package com.zexqm.rpgproject.rpg;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class CrowdControlResolver {
    public static boolean canApply(ServerPlayer target, CrowdControlType type, Vec3 attackOrigin) {
        return target.getCapability(RpgPlayerDataProvider.DATA).map(data -> {
            ProtectionState protection = data.protection();
            if (protection.iframe()) return false;
            if (type == CrowdControlType.GRAB) return !protection.grabImmune();
            if (protection.superArmor()) return false;
            return !protection.frontGuard() || !isFrontal(target, attackOrigin);
        }).orElse(true);
    }

    public static boolean isFrontal(ServerPlayer target, Vec3 origin) {
        if (origin == null) return false;
        Vec3 forward = target.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 towardAttack = origin.subtract(target.position()).multiply(1, 0, 1).normalize();
        return forward.lengthSqr() > 0 && towardAttack.lengthSqr() > 0 && forward.dot(towardAttack) >= 0;
    }
    private CrowdControlResolver() {}
}
