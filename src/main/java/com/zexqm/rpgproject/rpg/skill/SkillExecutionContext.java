package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public record SkillExecutionContext(ServerPlayer caster, Vec3 origin, Vec3 direction,
                                    Integer targetEntityId, Vec3 groundPosition, int lateralSide) {
    public SkillExecutionContext {
        if (caster == null || origin == null || direction == null) throw new IllegalArgumentException("Missing cast context");
        direction = direction.lengthSqr() < 1.0E-6 ? caster.getLookAngle() : direction.normalize();
        if (lateralSide < -1 || lateralSide > 1) throw new IllegalArgumentException("Invalid lateral side");
    }

    public SkillExecutionContext(ServerPlayer caster, Vec3 origin, Vec3 direction,
                                 Integer targetEntityId, Vec3 groundPosition) {
        this(caster, origin, direction, targetEntityId, groundPosition, 0);
    }
}
