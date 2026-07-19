package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HealingAuraProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_healing_aura");

    @Test void ranksPreserveSupportContract() throws Exception {
        assertRank(1, 300, 0.12);
        assertRank(2, 280, 0.14);
        assertRank(3, 260, 0.16);
        assertRank(4, 240, 0.18);
        assertRank(5, 200, 0.20);
    }

    private void assertRank(int rank, int cooldown, double recovery) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_healing_aura_rank_" + rank + ".json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            var skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(SkillTargetingType.ENTITY_TARGETED, skill.targeting());
            assertEquals(0, skill.resourceCost());
            assertEquals(cooldown, skill.cooldownTicks());
            assertEquals(recovery, skill.castMpRecoveryPercent());
            assertEquals(1, skill.hits().size());
            var hit = skill.hits().get(0);
            assertEquals(SkillTargetDisposition.SELF_AND_ALLY, hit.targetDisposition());
            assertEquals(2, hit.maxTargets());
            assertEquals(RpgPowerType.NONE, hit.powerType());
            assertEquals(0, hit.coefficient());
            assertEquals(recovery, hit.health().maxHealthRecoveryPercent());
            assertTrue(hit.health().active());
        }
    }
}
