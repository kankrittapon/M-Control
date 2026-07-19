package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlizzardProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_blizzard");

    @Test
    void productionRanksPreserveSevenPulseChannelAndSourceValues() throws Exception {
        assertRank(1, 250, 2400, 2.8071, 0.025, 0.01);
        assertRank(2, 300, 2000, 2.9223, 0.075, 0.015);
        assertRank(3, 350, 1600, 3.0332, 0.125, 0.02);
        assertRank(4, 150, 1200, 3.1401, 0.175, 0.025);
    }

    private static void assertRank(int rank, int mp, int cooldown, double coefficient,
                                   double accuracy, double targetDrain) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_blizzard_rank_" + rank + ".json";
        try (var stream = BlizzardProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID,
                        JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(rank, skill.rank());
                assertEquals(mp, skill.resourceCost());
                assertEquals(cooldown, skill.cooldownTicks());
                assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
                assertEquals(MovementPolicy.LOCKED, skill.movementPolicy());
                assertEquals(7.0, skill.radius());
                assertEquals(List.of(8, 12, 16, 20, 24, 28, 32), skill.hits().stream()
                        .map(SkillDefinition.Hit::timingTick).toList());
                assertTrue(skill.protectionWindows().isEmpty());
                assertTrue(skill.transitions().allows(SkillCancelTrigger.MOVEMENT, 0, 8));
                assertTrue(skill.transitions().allows(SkillCancelTrigger.SKILL, 0, 8));

                for (SkillDefinition.Hit hit : skill.hits()) {
                    assertEquals(coefficient, hit.coefficient());
                    assertEquals(accuracy, hit.hitChanceBonus());
                    assertEquals(10, hit.maxTargets());
                    assertEquals(targetDrain, hit.resources().targetMaxResourceDrainPercent());
                    assertEquals(1, hit.statuses().size());
                    assertEquals(RpgStatusType.SLOW, hit.statuses().get(0).type());
                    assertEquals(0.20, hit.statuses().get(0).potency());
                    assertNull(hit.crowdControl());
                }
            }
        }
    }
}
