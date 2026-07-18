package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ManaAbsorptionProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_mana_absorption");

    @Test
    void productionRanksKeepAuditedRecoveryAndDeliveryContract() throws Exception {
        assertRank(1, 240, 0.10, 4, 0.9905);
        assertRank(2, 180, 0.20, 7, 1.1189);
        assertRank(3, 140, 0.30, 10, 1.1649);
    }

    private static void assertRank(int rank, int cooldown, double recovery,
                                   int maxTargets, double coefficient) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_mana_absorption_rank_" + rank + ".json";
        try (var stream = ManaAbsorptionProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID,
                        JsonParser.parseReader(reader).getAsJsonObject());

                assertEquals(rank, skill.rank());
                assertEquals(0, skill.resourceCost());
                assertEquals(recovery, skill.castMpRecoveryPercent());
                assertEquals(cooldown, skill.cooldownTicks());
                assertEquals(14, skill.castTicks());
                assertEquals(6, skill.recoveryTicks());
                assertEquals(SkillTargetingType.CONE, skill.targeting());
                assertEquals(MovementPolicy.ROTATE_ONLY, skill.movementPolicy());
                assertEquals(FacingPolicy.TRACK_AIM_UNTIL_RELEASE, skill.facingPolicy());
                assertEquals(8.0, skill.range());
                assertEquals(2, skill.hits().size());
                assertEquals(8, skill.hits().get(0).timingTick());
                assertEquals(12, skill.hits().get(1).timingTick());
                for (SkillDefinition.Hit hit : skill.hits()) {
                    assertEquals(coefficient, hit.coefficient());
                    assertEquals(maxTargets, hit.maxTargets());
                    assertEquals(1, hit.statuses().size());
                    var slow = hit.statuses().get(0);
                    assertEquals(RpgStatusType.SLOW, slow.type());
                    assertEquals(100, slow.durationTicks());
                    assertEquals(0.20, slow.potency());
                }
                assertEquals(1, skill.protectionWindows().size());
                var guard = skill.protectionWindows().get(0);
                assertEquals(ProtectionType.FRONT_GUARD, guard.type());
                assertEquals(0, guard.fromTick());
                assertEquals(12, guard.toTick());
            }
        }
    }
}
