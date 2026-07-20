package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProtectedAreaProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_protected_area");

    @Test void ranksPreserveCurrentAreaProtectionContract() throws Exception {
        int[] mp = {70, 75, 80, 85, 90};
        int[] cooldown = {4800, 4400, 4000, 3600, 3000};
        for (int rank = 1; rank <= 5; rank++) assertRank(rank, mp[rank - 1], cooldown[rank - 1], rank * 20 + 60);
    }

    private void assertRank(int rank, int mp, int cooldown, int duration) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_protected_area_rank_" + rank + ".json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            var skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
            assertEquals(mp, skill.resourceCost());
            assertEquals(cooldown, skill.cooldownTicks());
            assertEquals(15, skill.radius());
            assertEquals(1, skill.hits().size());
            var hit = skill.hits().get(0);
            assertEquals(SkillTargetDisposition.SELF_AND_ALLY, hit.targetDisposition());
            assertEquals(11, hit.maxTargets());
            assertEquals(duration, hit.defensive().damageReductionTicks());
            assertEquals(0.50, hit.defensive().damageReductionRatio());
            assertTrue(skill.protectionWindows().stream()
                    .anyMatch(window -> window.type() == ProtectionType.SUPER_ARMOR
                            && window.active(0) && window.active(12)));
        }
    }
}
