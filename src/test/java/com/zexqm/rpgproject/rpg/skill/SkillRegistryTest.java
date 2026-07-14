package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillRegistryTest {
    @Test
    void parsesMultiHitProtectionAndStatusProfile() {
        String json = """
                {"debug_only":true,"targeting":"cone","range":6,
                 "hits":[{"timing_tick":2,"statuses":[{"type":"burn","duration_ticks":40,
                   "potency":1,"allowed_profiles":["normal","boss"]}]}],
                 "protection_windows":[{"type":"super_armor","from_tick":0,"to_tick":2}]}
                """;
        SkillDefinition skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "test"),
                JsonParser.parseString(json).getAsJsonObject());
        assertEquals(SkillTargetingType.CONE, skill.targeting());
        assertEquals(1, skill.hits().size());
        assertTrue(skill.hits().get(0).statuses().get(0).allowedProfiles().contains(MobControlProfile.BOSS));
        assertTrue(skill.protectionWindows().get(0).active(2));
    }

    @Test
    void rejectsNegativeTiming() {
        String json = "{\"debug_only\":true,\"targeting\":\"ray\",\"hits\":[{\"timing_tick\":-1}]}";
        assertThrows(IllegalArgumentException.class, () -> SkillRegistry.parse(
                new ResourceLocation("rpg_project", "bad"), JsonParser.parseString(json).getAsJsonObject()));
    }
}
