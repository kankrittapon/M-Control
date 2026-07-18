package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LightningProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_lightning");

    @Test
    void productionRanksKeepAuditedDamageAndGroundAoeContract() throws Exception {
        assertRank(1, 20, 140, 0.9716);
        assertRank(2, 30, 140, 1.0493);
        assertRank(3, 40, 120, 1.1221);
        assertRank(4, 50, 120, 1.1900);
        assertRank(5, 60, 100, 1.2546);
    }

    private static void assertRank(int rank, int mp, int cooldown, double coefficient) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_lightning_rank_" + rank + ".json";
        try (var stream = LightningProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID,
                        JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(rank, skill.rank());
                assertEquals(mp, skill.resourceCost());
                assertEquals(cooldown, skill.cooldownTicks());
                assertEquals(SkillTargetingType.GROUND_AOE, skill.targeting());
                assertEquals(20.0, skill.range());
                assertEquals(3.0, skill.radius());
                assertEquals(10, skill.castTicks());
                assertEquals(5, skill.recoveryTicks());
                assertEquals(3, skill.hits().size());
                for (int index = 0; index < skill.hits().size(); index++) {
                    var hit = skill.hits().get(index);
                    assertEquals(7 + index, hit.timingTick());
                    assertEquals(coefficient, hit.coefficient());
                    assertEquals(3.0, hit.radius());
                    assertEquals(10, hit.maxTargets());
                    assertTrue(hit.specialAttacks().contains(SpecialAttackType.DOWN_ATTACK));
                    assertEquals(RpgStatusType.SLOW, hit.statuses().get(0).type());
                    assertEquals(100, hit.statuses().get(0).durationTicks());
                }
                assertEquals(CrowdControlType.STUN, skill.hits().get(0).crowdControl());
                assertNull(skill.hits().get(1).crowdControl());
                assertNull(skill.hits().get(2).crowdControl());

                var recast = skill.cooldownRecast();
                assertTrue(recast.enabled());
                assertEquals(0.50, recast.damageMultiplier());
                assertFalse(recast.allowCrowdControl());
                assertTrue(recast.allowSpecialAttacks());
                assertTrue(recast.allowStatuses());
            }
        }
    }
}
