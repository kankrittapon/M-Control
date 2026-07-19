package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FreezeProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_freeze");

    @Test
    void productionRanksApplyFreezeOnlyAfterBothSourceHitsDealDamage() throws Exception {
        assertRank(1, 30, 160, 2, 4.0, 2.3281);
        assertRank(2, 35, 160, 3, 4.5, 2.4413);
        assertRank(3, 40, 140, 3, 5.0, 2.5515);
        assertRank(4, 45, 140, 4, 5.5, 2.6571);
        assertRank(5, 50, 120, 5, 6.0, 2.7586);
    }

    private static void assertRank(int rank, int mp, int cooldown, int targets,
                                   double range, double coefficient) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_freeze_rank_" + rank + ".json";
        try (var stream = FreezeProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID,
                        JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(rank, skill.rank());
                assertEquals(mp, skill.resourceCost());
                assertEquals(cooldown, skill.cooldownTicks());
                assertEquals(SkillTargetingType.CONE, skill.targeting());
                assertEquals(range, skill.range());
                assertEquals(2, skill.hits().size());
                assertEquals(coefficient, skill.hits().get(0).coefficient());
                assertEquals(coefficient, skill.hits().get(1).coefficient());
                assertEquals(targets, skill.hits().get(0).maxTargets());
                assertEquals(targets, skill.hits().get(1).maxTargets());
                assertNull(skill.hits().get(0).crowdControl(),
                        "The first hit must not freeze and block the second source hit");
                assertEquals(CrowdControlType.FREEZE, skill.hits().get(1).crowdControl());
            }
        }
    }
}
