package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LightningStormProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_lightning_storm");

    @Test
    void productionRanksPreserveFivePulseCriticalStorm() throws Exception {
        assertRank(1, 100, 280, 4, 1.8520, 0.60, 0.075);
        assertRank(2, 125, 260, 5, 2.1610, 0.67, 0.10);
        assertRank(3, 150, 240, 6, 2.4310, 0.75, 0.125);
    }

    private static void assertRank(int rank, int mp, int cooldown, int targets,
                                   double coefficient, double critical, double accuracy) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_lightning_storm_rank_" + rank + ".json";
        try (var stream = LightningStormProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID,
                        JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(rank, skill.rank());
                assertEquals(mp, skill.resourceCost());
                assertEquals(cooldown, skill.cooldownTicks());
                assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
                assertEquals(6.5, skill.radius());
                assertEquals(17, skill.castTicks());
                assertEquals(List.of(4, 7, 10, 13, 16), skill.hits().stream()
                        .map(SkillDefinition.Hit::timingTick).toList());
                assertTrue(skill.protectionWindows().isEmpty());
                for (int index = 0; index < skill.hits().size(); index++) {
                    SkillDefinition.Hit hit = skill.hits().get(index);
                    assertEquals(coefficient, hit.coefficient());
                    assertEquals(critical, hit.criticalChanceBonus());
                    assertEquals(accuracy, hit.hitChanceBonus());
                    assertEquals(targets, hit.maxTargets());
                    assertEquals(index == 0 ? CrowdControlType.STIFFNESS : null, hit.crowdControl());
                    assertEquals(RpgStatusType.SLOW, hit.statuses().get(0).type());
                }
            }
        }
    }
}
