package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResidualLightningProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_residual_lightning");
    private static final ResourceLocation LINK = new ResourceLocation("rpg_project", "residual_lightning_ready");

    @Test
    void productionRanksPreserveInitialAndFinishingHits() throws Exception {
        assertRank(1, 60, 300, 1.6733, 1.8330, 0.50, 0.01);
        assertRank(2, 70, 280, 1.9416, 2.1270, 0.57, 0.012);
        assertRank(3, 80, 260, 2.1772, 2.3850, 0.64, 0.015);
        assertRank(4, 90, 220, 2.3896, 2.6176, 0.75, 0.02);
    }

    @Test
    void everyLightningRankGrantsTheResidualAnchorLink() throws Exception {
        ResourceLocation lightning = new ResourceLocation("rpg_project", "wizard_lightning");
        for (int rank = 1; rank <= 5; rank++) {
            String path = "/data/rpg_project/rpg_skills/wizard_lightning_rank_" + rank + ".json";
            try (var stream = ResidualLightningProductionDefinitionTest.class.getResourceAsStream(path)) {
                assertNotNull(stream);
                SkillDefinition skill = SkillRegistry.parse(lightning, JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
                assertEquals(LINK, skill.links().grants());
                assertEquals(100, skill.links().grantDurationTicks());
                assertEquals(SkillLinkTiming.SUCCESSFUL_HIT, skill.links().grantTiming());
            }
        }
    }

    private static void assertRank(int rank, int mp, int cooldown, double initial,
                                   double finisher, double critical, double drain) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_residual_lightning_rank_" + rank + ".json";
        try (var stream = ResidualLightningProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream);
            SkillDefinition skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(mp, skill.resourceCost());
            assertEquals(cooldown, skill.cooldownTicks());
            assertEquals(List.of(2, 3, 4, 5, 6, 8, 9), skill.hits().stream()
                    .map(SkillDefinition.Hit::timingTick).toList());
            assertEquals(LINK, skill.links().requires());
            assertTrue(skill.links().consumeRequiredOnCastStart());
            assertTrue(skill.links().useRequiredAnchor());
            for (int index = 0; index < 7; index++) {
                var hit = skill.hits().get(index);
                assertEquals(index < 5 ? initial : finisher, hit.coefficient());
                assertEquals(critical, hit.criticalChanceBonus());
                assertEquals(index == 0 ? CrowdControlType.BOUND : null, hit.crowdControl());
                assertEquals(index < 5 ? 0.0 : drain, hit.resources().targetMaxResourceDrainPercent());
            }
        }
    }
}
