package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StaffAttackProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_staff_attack");
    private static final double[] COEFFICIENTS = {
            1.0, 1.118, 1.2247, 1.3229, 1.4142, 1.5, 1.5811, 1.6583, 1.7321, 1.8028
    };
    private static final int[] MP_RECOVERY = {5, 6, 7, 8, 9, 10, 11, 12, 13, 15};

    @Test void allRanksPreserveInnateLmbMeleeContract() throws Exception {
        for (int rank = 1; rank <= 10; rank++) {
            String path = "/data/rpg_project/rpg_skills/wizard_staff_attack_rank_" + rank + ".json";
            try (var stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, path);
                var skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
                assertTrue(skill.innate());
                assertEquals(SkillTargetingType.RAY, skill.targeting());
                assertEquals(MovementPolicy.WALK, skill.movementPolicy());
                assertEquals(0, skill.resourceCost());
                assertEquals(0, skill.cooldownTicks());
                assertEquals(3.0, skill.range());
                assertEquals(1, skill.hits().size());
                var hit = skill.hits().get(0);
                assertEquals(COEFFICIENTS[rank - 1], hit.coefficient());
                assertEquals(rank * 0.04, hit.hitChanceBonus(), 1.0e-9);
                assertEquals(MP_RECOVERY[rank - 1], hit.resources().flatMpRecovery());
                assertEquals(1, hit.maxTargets());
                assertTrue(hit.specialAttacks().contains(SpecialAttackType.DOWN_ATTACK));
            }
        }
    }
}
