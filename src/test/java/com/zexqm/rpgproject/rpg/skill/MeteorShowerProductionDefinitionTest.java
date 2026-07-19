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

class MeteorShowerProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_meteor_shower");

    @Test
    void productionRanksPreserveFourImpactGroundAoeContract() throws Exception {
        assertRank(1, 200, 2600, 36, 3.8807, 4.0050, 30.0);
        assertRank(2, 250, 2200, 32, 4.4497, 4.6217, 40.0);
        assertRank(3, 300, 1800, 28, 4.9538, 5.0000, 50.0);
    }

    private static void assertRank(int rank, int mp, int cooldown, int castTicks,
                                   double opening, double meteor, double burn) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_meteor_shower_rank_" + rank + ".json";
        try (var stream = MeteorShowerProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID,
                        JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(rank, skill.rank());
                assertEquals(mp, skill.resourceCost());
                assertEquals(cooldown, skill.cooldownTicks());
                assertEquals(castTicks, skill.castTicks());
                assertEquals(SkillTargetingType.GROUND_AOE, skill.targeting());
                assertEquals(MovementPolicy.LOCKED, skill.movementPolicy());
                assertEquals(24.0, skill.range());
                assertEquals(4, skill.hits().size());
                assertEquals(opening, skill.hits().get(0).coefficient());
                assertEquals(opening, skill.hits().get(1).coefficient());
                assertEquals(meteor, skill.hits().get(2).coefficient());
                assertEquals(meteor, skill.hits().get(3).coefficient());
                assertEquals(CrowdControlType.STIFFNESS, skill.hits().get(0).crowdControl());
                assertNull(skill.hits().get(1).crowdControl());
                assertEquals(CrowdControlType.KNOCKDOWN, skill.hits().get(2).crowdControl());
                assertNull(skill.hits().get(3).crowdControl());
                for (var hit : skill.hits()) {
                    assertEquals(4.5, hit.radius());
                    assertEquals(10, hit.maxTargets());
                    assertTrue(hit.specialAttacks().contains(SpecialAttackType.DOWN_ATTACK));
                    assertEquals(RpgStatusType.BURN, hit.statuses().get(0).type());
                    assertEquals(360, hit.statuses().get(0).durationTicks());
                    assertEquals(60, hit.statuses().get(0).intervalTicks());
                    assertEquals(burn, hit.statuses().get(0).potency());
                }
                assertEquals(ProtectionType.FRONT_GUARD, skill.protectionWindows().get(0).type());
                assertEquals(ProtectionType.SUPER_ARMOR, skill.protectionWindows().get(1).type());
                assertTrue(skill.transitions().movementUntilFirstHit());
            }
        }
    }
}
