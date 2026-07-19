package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HealingLighthouseProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_healing_lighthouse");

    @Test void ranksPreserveThreePulseSupportContract() throws Exception {
        assertRank(1, 100, 1800, 0.07, 0.05, 45);
        assertRank(2, 140, 1400, 0.10, 0.07, 75);
        assertRank(3, 180, 1000, 0.15, 0.10, 120);
        assertRank(4, 220, 600, 0.30, 0.20, 150);
    }

    private void assertRank(int rank, int cost, int cooldown, double selfHeal,
                            double allyHeal, int mpRecovery) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_healing_lighthouse_rank_" + rank + ".json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            var skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
            assertEquals(cost, skill.resourceCost());
            assertEquals(cooldown, skill.cooldownTicks());
            assertEquals(3, skill.hits().size());
            assertEquals(java.util.List.of(8, 16, 24), skill.hits().stream()
                    .map(SkillDefinition.Hit::timingTick).toList());
            for (var hit : skill.hits()) {
                assertEquals(SkillTargetDisposition.SELF_AND_ALLY, hit.targetDisposition());
                assertEquals(11, hit.maxTargets());
                assertEquals(RpgPowerType.NONE, hit.powerType());
                assertEquals(0, hit.coefficient());
                assertEquals(selfHeal, hit.health().maxHealthRecoveryPercent());
                assertEquals(allyHeal, hit.health().allyMaxHealthRecoveryPercent());
                assertEquals(mpRecovery, hit.resources().flatMpRecovery());
                assertTrue(hit.resources().recoverOncePerHitWindow());
            }
        }
    }
}
