package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillHitResolverTest {
    private static final AABB ZOMBIE_BOUNDS = new AABB(-0.3, 0.0, 4.7, 0.3, 1.95, 5.3);

    @Test
    void lineAimedAtEyeHeightIntersectsEntityBounds() {
        assertTrue(SkillHitResolver.intersectsLine(
                ZOMBIE_BOUNDS,
                new Vec3(0.0, 1.62, 0.0),
                new Vec3(0.0, 1.62, 10.0),
                0.0));
    }

    @Test
    void lineOutsideEntityAndSkillRadiusMisses() {
        assertFalse(SkillHitResolver.intersectsLine(
                ZOMBIE_BOUNDS,
                new Vec3(2.0, 1.62, 0.0),
                new Vec3(2.0, 1.62, 10.0),
                1.0));
    }
}
