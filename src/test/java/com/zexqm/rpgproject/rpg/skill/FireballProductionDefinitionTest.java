package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FireballProductionDefinitionTest {
    private static final ResourceLocation FIREBALL = id("wizard_fireball");
    private static final ResourceLocation EXPLOSION = id("wizard_fireball_explosion");
    private static final ResourceLocation LINK = id("fireball_explosion_ready");

    @Test
    void everyFireballRankKeepsQuickProjectileContract() {
        int[] costs = {10, 15, 20, 30};
        int[] cooldowns = {100, 80, 80, 60};
        int[] targetCaps = {4, 5, 6, 7};
        for (int rank = 1; rank <= 4; rank++) {
            SkillDefinition skill = load("wizard_fireball_rank_" + rank, FIREBALL);
            assertEquals(rank, skill.rank());
            assertEquals(SkillTargetingType.AIM_PROJECTILE, skill.targeting());
            assertEquals(costs[rank - 1], skill.resourceCost());
            assertEquals(cooldowns[rank - 1], skill.cooldownTicks());
            assertEquals(2, skill.castTicks());
            assertEquals(1, skill.hits().get(0).timingTick());
            assertEquals(3, skill.recoveryTicks());
            assertEquals(24.0, skill.range());
            assertEquals(1.25, skill.projectileSpeed());
            assertEquals(1.75, skill.hits().get(0).radius());
            assertEquals(targetCaps[rank - 1], skill.hits().get(0).maxTargets());
            assertEquals(FacingPolicy.TRACK_AIM_UNTIL_RELEASE, skill.facingPolicy());
            assertEquals(90.0, skill.turnSpeed());
            assertFalse(skill.transitions().allows(SkillCancelTrigger.MOVEMENT, 0, 1));
            assertEquals(LINK, skill.links().grants());
            assertEquals(140, skill.links().grantDurationTicks());
        }
    }

    @Test
    void everyExplosionRankRequiresAndConsumesResolvedFireballAnchor() {
        int[] costs = {65, 75, 85};
        int[] cooldowns = {300, 280, 260};
        for (int rank = 1; rank <= 3; rank++) {
            SkillDefinition skill = load("wizard_fireball_explosion_rank_" + rank, EXPLOSION);
            assertEquals(rank, skill.rank());
            assertEquals(SkillTargetingType.GROUND_AOE, skill.targeting());
            assertEquals(costs[rank - 1], skill.resourceCost());
            assertEquals(cooldowns[rank - 1], skill.cooldownTicks());
            assertEquals(4, skill.castTicks());
            assertEquals(3, skill.hits().get(0).timingTick());
            assertEquals(8, skill.recoveryTicks());
            assertEquals(2.75, skill.hits().get(0).radius());
            assertEquals(10, skill.hits().get(0).maxTargets());
            assertFalse(skill.transitions().allows(SkillCancelTrigger.MOVEMENT, 0, 3));
            assertEquals(LINK, skill.links().requires());
            assertTrue(skill.links().consumeRequiredOnCastStart());
            assertTrue(skill.links().useRequiredAnchor());
        }
    }

    private static SkillDefinition load(String fileName, ResourceLocation stableId) {
        String path = "/data/rpg_project/rpg_skills/" + fileName + ".json";
        var stream = FireballProductionDefinitionTest.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing production skill resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return SkillRegistry.parse(stableId, JsonParser.parseReader(reader).getAsJsonObject());
        } catch (java.io.IOException exception) {
            throw new AssertionError("Unable to read " + path, exception);
        }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("rpg_project", path);
    }
}
