package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class EarthsResponseProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_earth_s_response");

    @Test void ranksPreserveLateralLineContract() throws Exception {
        assertRank(1, 30, 80, 3, 1.4933, 0.05, 1.5);
        assertRank(2, 40, 80, 5, 1.8330, 0.10, 1.75);
        assertRank(3, 50, 60, 7, 2.3043, 0.15, 2.0);
    }

    private void assertRank(int rank, int mp, int cooldown, int targets, double coefficient,
                            double accuracy, double lateral) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_earth_s_response_rank_" + rank + ".json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            var skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(SkillTargetingType.LINE, skill.targeting());
            assertEquals(mp, skill.resourceCost());
            assertEquals(cooldown, skill.cooldownTicks());
            assertEquals(CasterMovementType.SIDE_HOP, skill.casterMovementType());
            assertEquals(lateral, skill.casterLateralDistance());
            assertEquals(2, skill.hits().size());
            for (var hit : skill.hits()) {
                assertEquals(targets, hit.maxTargets());
                assertEquals(coefficient, hit.coefficient());
                assertEquals(accuracy, hit.hitChanceBonus());
            }
            assertEquals(CrowdControlType.FLOAT, skill.hits().get(0).crowdControl());
            assertNull(skill.hits().get(1).crowdControl());
        }
    }
}
