package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeleportProductionDefinitionTest {
    @Test void preservesAuditedRankContract() throws Exception {
        List<Integer> cooldowns = List.of(180, 160, 140);
        for (int rank = 1; rank <= 3; rank++) {
            String path = "/data/rpg_project/rpg_skills/wizard_teleport_rank_" + rank + ".json";
            try (var stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, path);
                var skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "wizard_teleport"),
                        JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
                assertEquals(20.0, skill.staminaCost());
                assertEquals(cooldowns.get(rank - 1), skill.cooldownTicks());
                assertEquals(CasterMovementType.TELEPORT, skill.casterMovementType());
                assertEquals(8.0, skill.casterLateralDistance());
                assertEquals(SkillAimMode.CONFIRM_TARGETING, skill.aimMode());
                assertEquals(100, skill.targetingTimeoutTicks());
                assertEquals(FacingPolicy.AIM_ON_CAST, skill.facingPolicy());
                assertEquals(ProtectionType.IFRAME, skill.protectionWindows().get(0).type());
                assertEquals(200, skill.hits().get(0).defensive().speedBuffTicks());
                assertEquals(0.10, skill.hits().get(0).defensive().timedMovementSpeedBonus());
            }
        }
    }
}
