package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FrigidFogProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_frigid_fog");

    @Test
    void productionRanksPreserveSourceDamageProtectionAndResourceDrain() throws Exception {
        assertRank(1, 120, 380, 1.4318, 0.10, 0.02);
        assertRank(2, 130, 340, 1.7088, 0.12, 0.03);
        assertRank(3, 140, 320, 1.9468, 0.14, 0.04);
        assertRank(4, 160, 300, 2.1587, 0.16, 0.05);
    }

    private static void assertRank(int rank, int mp, int cooldown, double coefficient,
                                   double accuracy, double resourceDrain) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_frigid_fog_rank_" + rank + ".json";
        try (var stream = FrigidFogProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID,
                        JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(rank, skill.rank());
                assertEquals(mp, skill.resourceCost());
                assertEquals(cooldown, skill.cooldownTicks());
                assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
                assertEquals(MovementPolicy.LOCKED, skill.movementPolicy());
                assertEquals(5.0, skill.radius());
                assertEquals(3, skill.hits().size());
                assertEquals(ProtectionType.SUPER_ARMOR, skill.protectionWindows().get(0).type());

                for (int index = 0; index < skill.hits().size(); index++) {
                    SkillDefinition.Hit hit = skill.hits().get(index);
                    assertEquals(coefficient, hit.coefficient());
                    assertEquals(accuracy, hit.hitChanceBonus());
                    assertEquals(10, hit.maxTargets());
                    assertTrue(hit.specialAttacks().contains(SpecialAttackType.DOWN_ATTACK));
                    if (index < 2) {
                        assertNull(hit.crowdControl());
                        assertEquals(0, hit.resources().targetMaxResourceDrainPercent());
                    }
                }

                SkillDefinition.Hit finalHit = skill.hits().get(2);
                assertEquals(CrowdControlType.FREEZE, finalHit.crowdControl());
                assertEquals(resourceDrain, finalHit.resources().targetMaxResourceDrainPercent());
            }
        }
    }
}
