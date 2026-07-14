package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillLinkStateTest {
    private static final ResourceLocation FIREBALL = new ResourceLocation("rpg_project", "fireball_ready");

    @Test
    void grantsExtendsAndExpiresByServerGameTime() {
        SkillLinkState state = new SkillLinkState();
        state.grant(FIREBALL, 140);
        state.grant(FIREBALL, 100);
        assertTrue(state.has(FIREBALL, 139));
        assertEquals(1, state.remaining(FIREBALL, 139));
        assertFalse(state.has(FIREBALL, 140));
    }

    @Test
    void consumeIsAtomicAndMissingRequirementIsNeutral() {
        SkillLinkState state = new SkillLinkState();
        assertTrue(state.has(null, 0));
        state.grant(FIREBALL, 20);
        assertTrue(state.consume(FIREBALL, 10));
        assertFalse(state.consume(FIREBALL, 10));
    }

    @Test
    void anchorFollowsLinkLifetimeAndConsumption() {
        SkillLinkState state = new SkillLinkState();
        Vec3 impact = new Vec3(2, 3, 4);
        state.grant(FIREBALL, 20);
        state.setAnchor(FIREBALL, impact);
        assertEquals(impact, state.anchor(FIREBALL, 10));
        assertTrue(state.consume(FIREBALL, 10));
        assertNull(state.anchor(FIREBALL, 10));
    }
}
