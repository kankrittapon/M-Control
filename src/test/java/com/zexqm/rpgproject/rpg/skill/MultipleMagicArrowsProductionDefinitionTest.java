package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MultipleMagicArrowsProductionDefinitionTest {
    @Test
    void productionDefinitionKeepsAuditedAndProvisionalContract() throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_multiple_magic_arrows_rank_1.json";
        try (var stream = MultipleMagicArrowsProductionDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing production skill resource " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SkillDefinition skill = SkillRegistry.parse(
                        new ResourceLocation("rpg_project", "wizard_multiple_magic_arrows"),
                        JsonParser.parseReader(reader).getAsJsonObject());

                assertEquals(1, skill.rank());
                assertEquals(100, skill.resourceCost());
                assertEquals(220, skill.cooldownTicks());
                assertEquals(5, skill.castTicks());
                assertEquals(5, skill.recoveryTicks());
                assertEquals(SkillTargetingType.AIM_PROJECTILE, skill.targeting());
                assertEquals(MovementPolicy.WALK, skill.movementPolicy());
                assertEquals(FacingPolicy.TRACK_AIM_UNTIL_RELEASE, skill.facingPolicy());
                assertEquals(24.0, skill.range());
                assertEquals(1.5, skill.projectileSpeed());
                assertEquals(2, skill.hits().size());
                assertEquals(3, skill.hits().get(0).timingTick());
                assertEquals(4, skill.hits().get(1).timingTick());
                for (SkillDefinition.Hit hit : skill.hits()) {
                    assertEquals(1.2574, hit.coefficient());
                    assertEquals(1.5, hit.radius());
                    assertEquals(10, hit.maxTargets());
                    assertEquals(1.0, hit.criticalChanceBonus());
                }

                var recast = skill.cooldownRecast();
                assertTrue(recast.enabled());
                assertEquals(0.35, recast.damageMultiplier());
                assertFalse(recast.allowCritical());
                assertFalse(recast.allowSpecialAttacks());
                assertFalse(recast.allowCrowdControl());
                assertFalse(recast.allowSmash());
                assertFalse(recast.allowStatuses());
                assertFalse(recast.allowResources());
                assertFalse(recast.allowProtection());
            }
        }
    }
}
