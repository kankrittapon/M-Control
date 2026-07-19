package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MagicalShieldProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_magical_shield");

    @Test void ranksPreserveCurrentManaShieldContract() throws Exception {
        assertRank(1, 0.10, 0.12);
        assertRank(2, 0.15, 0.18);
        assertRank(3, 0.20, 0.24);
        assertRank(4, 0.25, 0.30);
    }

    private void assertRank(int rank, double shieldRatio, double resistance) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_magical_shield_rank_" + rank + ".json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            var skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
            assertEquals(2400, skill.cooldownTicks());
            assertEquals(1, skill.hits().size());
            var hit = skill.hits().get(0);
            assertEquals(SkillTargetDisposition.SELF, hit.targetDisposition());
            assertEquals(1200, hit.defensive().manaShieldTicks());
            assertEquals(shieldRatio, hit.defensive().manaShieldRatio());
            assertEquals(600, hit.defensive().resistanceTicks());
            assertEquals(resistance, hit.defensive().resistanceBonus());
        }
    }
}
