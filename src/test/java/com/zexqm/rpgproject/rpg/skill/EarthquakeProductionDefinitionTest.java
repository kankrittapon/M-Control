package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EarthquakeProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_earthquake");

    @Test
    void productionRanksPreserveAuditedPulseCountsAndResources() throws Exception {
        assertRank(1, 100, 2400, 4, 3.0594);
        assertRank(2, 140, 2000, 5, 3.4986);
        assertRank(3, 180, 1600, 6, 3.8717);
        assertRank(4, 220, 1200, 6, 4.5398);
    }

    private static void assertRank(int rank, int mp, int cooldown, int hitCount,
                                   double coefficient) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_earthquake_rank_" + rank + ".json";
        try (var stream = EarthquakeProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID,
                        JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(rank, skill.rank());
                assertEquals(mp, skill.resourceCost());
                assertEquals(cooldown, skill.cooldownTicks());
                assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
                assertEquals(MovementPolicy.LOCKED, skill.movementPolicy());
                assertEquals(6.0, skill.radius());
                assertEquals(hitCount, skill.hits().size());
                assertEquals(ProtectionType.SUPER_ARMOR, skill.protectionWindows().get(0).type());

                for (int index = 0; index < hitCount; index++) {
                    SkillDefinition.Hit hit = skill.hits().get(index);
                    assertEquals(coefficient, hit.coefficient());
                    assertEquals(7, hit.maxTargets());
                    assertEquals(0.18, hit.pullStrength());
                    assertEquals(15, hit.resources().flatMpRecovery());
                    assertTrue(hit.specialAttacks().contains(SpecialAttackType.DOWN_ATTACK));
                    assertEquals(index == 0 ? CrowdControlType.BOUND : null, hit.crowdControl());
                    assertEquals(index == 0 ? CrowdControlType.STIFFNESS : null,
                            hit.playerCrowdControl());
                }
            }
        }
    }
}
