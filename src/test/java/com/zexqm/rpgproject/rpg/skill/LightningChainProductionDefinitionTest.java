package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LightningChainProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_lightning_chain");

    @Test
    void productionRanksKeepAuditedChannelAndChainContract() throws Exception {
        assertRank(1, 20, 3, 0.025, 2.2847);
        assertRank(2, 35, 4, 0.0375, 2.4203);
        assertRank(3, 50, 5, 0.05, 2.5478);
        assertRank(4, 65, 6, 0.0625, 2.6698);
    }

    private static void assertRank(int rank, int mp, int targets, double accuracy,
                                   double pulseCoefficient) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_lightning_chain_rank_" + rank + ".json";
        try (var stream = LightningChainProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(ID, JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(rank, skill.rank());
                assertEquals(mp, skill.resourceCost());
                assertEquals(SkillTargetingType.CHAIN, skill.targeting());
                assertEquals(16.0, skill.range());
                assertEquals(4.0, skill.radius());
                assertEquals(FacingPolicy.TRACK_AIM_UNTIL_RELEASE, skill.facingPolicy());
                assertEquals(2, skill.hits().size());
                assertEquals(CrowdControlType.STIFFNESS, skill.hits().get(0).crowdControl());
                assertNull(skill.hits().get(1).crowdControl());
                for (var hit : skill.hits()) {
                    assertEquals(targets, hit.maxTargets());
                    assertEquals(accuracy, hit.hitChanceBonus());
                    assertEquals(pulseCoefficient, hit.coefficient());
                    assertEquals(RpgStatusType.SLOW, hit.statuses().get(0).type());
                    assertEquals(1.0, hit.targetDamageMultiplier(0));
                    assertEquals(0.85, hit.targetDamageMultiplier(1));
                    assertEquals(0.70, hit.targetDamageMultiplier(2));
                    assertEquals(0.55, hit.targetDamageMultiplier(3));
                    assertEquals(0.40, hit.targetDamageMultiplier(4));
                    assertEquals(0.40, hit.targetDamageMultiplier(5));
                }
                assertEquals(new ResourceLocation("rpg_project", "lightning_storm_ready"),
                        skill.links().grants());
                assertEquals(140, skill.links().grantDurationTicks());
                assertEquals(SkillLinkTiming.CAST_COMPLETE, skill.links().grantTiming());
            }
        }
    }
}
