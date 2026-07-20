package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import com.zexqm.rpgproject.rpg.combat.RpgCombatState;

class SagesMemoryProductionDefinitionTest {
    @Test void preservesAuditedInstantCastContract() throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_sage_s_memory_rank_1.json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            var skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "wizard_sage_s_memory"),
                    JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(0, skill.resourceCost());
            assertEquals(4200, skill.cooldownTicks());
            assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
            assertEquals(300, skill.hits().get(0).defensive().castTimeOverrideTicks());
            assertEquals(SkillTargetDisposition.SELF, skill.hits().get(0).targetDisposition());
        }
    }

    @Test void activeOverrideRemovesOnlyPreHitWindup() throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_fireball_rank_4.json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            var fireball = SkillRegistry.parse(new ResourceLocation("rpg_project", "wizard_fireball"),
                    JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            int firstHit = fireball.hits().stream().mapToInt(SkillDefinition.Hit::timingTick).min().orElseThrow();
            RpgCombatState state = new RpgCombatState();
            assertEquals(0, SkillRuntime.castTimeOffset(fireball, state));
            state.activateCastTimeOverride(300);
            assertEquals(firstHit, SkillRuntime.castTimeOffset(fireball, state));
            assertEquals(fireball.castTicks() - firstHit,
                    fireball.castTicks() - SkillRuntime.castTimeOffset(fireball, state));
        }
    }
}
