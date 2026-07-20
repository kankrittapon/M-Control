package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SpellboundHeartProductionDefinitionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "wizard_spellbound_heart");

    @Test void ranksPreserveSustainedResourceContract() throws Exception {
        int[] duration = {6000, 7200, 8400, 9600, 12000};
        int[] recovery = {50, 100, 150, 200, 250};
        for (int rank = 1; rank <= 5; rank++) assertRank(rank, duration[rank - 1], recovery[rank - 1]);
    }

    private void assertRank(int rank, int duration, int recovery) throws Exception {
        String path = "/data/rpg_project/rpg_skills/wizard_spellbound_heart_rank_" + rank + ".json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            var skill = SkillRegistry.parse(ID, JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
            assertEquals(0, skill.resourceCost());
            assertEquals(600, skill.cooldownTicks());
            assertEquals(1, skill.hits().size());
            var payload = skill.hits().get(0).defensive();
            assertEquals(duration, payload.sustainedResourceTicks());
            assertEquals(200, payload.resourceIntervalTicks());
            assertEquals(recovery, payload.flatMpRecovery());
            assertEquals(0.10, payload.movementSpeedBonus());
        }
    }
}
