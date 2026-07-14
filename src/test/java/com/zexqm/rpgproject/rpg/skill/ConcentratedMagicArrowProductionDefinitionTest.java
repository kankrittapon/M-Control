package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ConcentratedMagicArrowProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation(
            "rpg_project", "wizard_concentrated_magic_arrow");

    @Test
    void everyRankMatchesAuditedCombatAndInitialDeliveryContract() {
        int[] mp = {40, 30, 20};
        int[] cooldown = {100, 100, 80};
        int[] targets = {3, 4, 5};
        double[] coefficients = {1.5937, 1.7516, 1.8963};
        double[] critical = {0.20, 0.30, 0.40};
        for (int rank = 1; rank <= 3; rank++) {
            SkillDefinition skill = load(rank);
            SkillDefinition.Hit hit = skill.hits().get(0);
            assertEquals(rank, skill.rank());
            assertEquals(SkillTargetingType.AIM_PROJECTILE, skill.targeting());
            assertEquals(mp[rank - 1], skill.resourceCost());
            assertEquals(cooldown[rank - 1], skill.cooldownTicks());
            assertEquals(6, skill.castTicks());
            assertEquals(5, hit.timingTick());
            assertEquals(4, skill.recoveryTicks());
            assertEquals(24.0, skill.range());
            assertEquals(1.5, skill.projectileSpeed());
            assertEquals(1.25, hit.radius());
            assertEquals(targets[rank - 1], hit.maxTargets());
            assertEquals(coefficients[rank - 1], hit.coefficient());
            assertEquals(critical[rank - 1], hit.criticalChanceBonus());
            assertEquals(FacingPolicy.TRACK_AIM_UNTIL_RELEASE, skill.facingPolicy());
            assertFalse(skill.transitions().allows(SkillCancelTrigger.MOVEMENT, 0, 5));
            assertFalse(skill.cooldownRecast().enabled());
        }
    }

    private static SkillDefinition load(int rank) {
        String path = "/data/rpg_project/rpg_skills/wizard_concentrated_magic_arrow_rank_"
                + rank + ".json";
        var stream = ConcentratedMagicArrowProductionDefinitionTest.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing production skill resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return SkillRegistry.parse(ID, JsonParser.parseReader(reader).getAsJsonObject());
        } catch (java.io.IOException exception) {
            throw new AssertionError("Unable to read " + path, exception);
        }
    }
}
