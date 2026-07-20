package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DaggerStabProductionDefinitionTest {
    @Test
    void ranksKeepAuditedDamageAndCooldownRecastRules() throws Exception {
        double[] coefficients = {2.3452, 2.5495, 2.7386, 2.9155, 3.0822};
        double[] accuracy = {0.04, 0.08, 0.12, 0.16, 0.20};
        for (int rank = 1; rank <= 5; rank++) {
            String path = "/data/rpg_project/rpg_skills/wizard_dagger_stab_rank_" + rank + ".json";
            try (var stream = DaggerStabProductionDefinitionTest.class.getResourceAsStream(path)) {
                assertNotNull(stream, "Missing production skill resource " + path);
                try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    SkillDefinition skill = SkillRegistry.parse(
                            new ResourceLocation("rpg_project", "wizard_dagger_stab"),
                            JsonParser.parseReader(reader).getAsJsonObject());

                    assertEquals(rank, skill.rank());
                    assertEquals(SkillTargetingType.RAY, skill.targeting());
                    assertTrue(skill.weapons().main());
                    assertTrue(skill.weapons().sub());
                    assertEquals(200, skill.cooldownTicks());
                    assertEquals(3.0, skill.range());
                    assertEquals(2, skill.hits().size());
                    for (SkillDefinition.Hit hit : skill.hits()) {
                        assertEquals(coefficients[rank - 1], hit.coefficient());
                        assertEquals(accuracy[rank - 1], hit.hitChanceBonus());
                        assertEquals(1.0, hit.criticalChanceBonus());
                        assertEquals(CrowdControlType.STIFFNESS, hit.crowdControl());
                        assertEquals(5, hit.maxTargets());
                        assertTrue(hit.specialAttacks().contains(SpecialAttackType.COUNTER_ATTACK));
                    }

                    var recast = skill.cooldownRecast();
                    assertTrue(recast.enabled());
                    assertEquals(0.35, recast.damageMultiplier());
                    assertFalse(recast.allowCritical());
                    assertFalse(recast.allowSpecialAttacks());
                    assertFalse(recast.allowCrowdControl());
                }
            }
        }
    }
}
