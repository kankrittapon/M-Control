package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Test
    void areaDistanceUsesClosestBoundingBoxPoint() {
        assertEquals(0.0, SkillHitResolver.distanceToBoundsSqr(
                new Vec3(0.0, 1.5, 5.0), ZOMBIE_BOUNDS));
        assertEquals(1.0, SkillHitResolver.distanceToBoundsSqr(
                new Vec3(1.3, 1.5, 5.0), ZOMBIE_BOUNDS), 1.0E-9);
    }

    @Test
    void boundedNearestPreservesCapAndDistanceOrderAtStressDensities() {
        for (int density : List.of(10, 25, 50)) {
            List<Double> distances = new ArrayList<>();
            for (int index = density; index >= 1; index--) distances.add((double) index);
            Collections.rotate(distances, density / 3);

            List<Double> selected = SkillHitResolver.boundedNearest(distances, 7);

            assertEquals(7, selected.size());
            assertEquals(List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0), selected);
        }
    }
}
