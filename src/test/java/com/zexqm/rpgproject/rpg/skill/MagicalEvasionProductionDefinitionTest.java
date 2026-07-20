package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MagicalEvasionProductionDefinitionTest {
    @Test void preservesAuditedRankContract() throws Exception {
        // BDO's 250/220/200/180/150 costs use a pool roughly ten times BRPG's Breath pool.
        List<Double> stamina = List.of(25.0, 22.0, 20.0, 18.0, 15.0);
        for (int rank = 1; rank <= 5; rank++) {
            String path = "/data/rpg_project/rpg_skills/wizard_magical_evasion_rank_" + rank + ".json";
            try (var stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, path);
                var skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "wizard_magical_evasion"),
                        JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
                assertEquals(stamina.get(rank - 1), skill.staminaCost());
                assertEquals(0, skill.cooldownTicks());
                assertEquals(CasterMovementType.OMNI_DODGE, skill.casterMovementType());
                assertEquals(2.0, skill.casterLateralDistance());
                assertTrue(skill.hits().isEmpty());
                assertEquals(ProtectionType.PVE_IFRAME, skill.protectionWindows().get(0).type());
            }
        }
    }
}
