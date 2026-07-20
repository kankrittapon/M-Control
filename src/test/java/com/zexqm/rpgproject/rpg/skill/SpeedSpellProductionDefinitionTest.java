package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SpeedSpellProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_speed_spell");

    @Test void ranksPreserveAuditedAreaSpeedContract() throws Exception {
        int[] mp = {120, 130, 140};
        int[] cooldown = {1800, 1500, 1000};
        double[] bonus = {0.10, 0.15, 0.20};
        for (int rank = 1; rank <= 3; rank++) {
            String path = "/data/rpg_project/rpg_skills/wizard_speed_spell_rank_" + rank + ".json";
            try (var stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream);
                var skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
                assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
                assertEquals(mp[rank - 1], skill.resourceCost());
                assertEquals(cooldown[rank - 1], skill.cooldownTicks());
                assertEquals(15.0, skill.radius());
                var hit = skill.hits().get(0);
                assertEquals(SkillTargetDisposition.SELF_AND_ALLY, hit.targetDisposition());
                assertEquals(11, hit.maxTargets());
                assertEquals(600, hit.defensive().speedBuffTicks());
                assertEquals(bonus[rank - 1], hit.defensive().attackSpeedBonus());
                assertEquals(bonus[rank - 1], hit.defensive().castingSpeedBonus());
                assertEquals(bonus[rank - 1], hit.defensive().timedMovementSpeedBonus());
            }
        }
    }
}
