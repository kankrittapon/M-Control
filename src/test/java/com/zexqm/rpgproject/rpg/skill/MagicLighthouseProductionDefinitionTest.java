package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MagicLighthouseProductionDefinitionTest {
    @Test
    void ranksKeepResourceCooldownAndTauntContract() throws Exception {
        int[] mp = {75, 65, 55};
        int[] cooldown = {800, 700, 600};
        int[] duration = {200, 300, 400};
        double[] radius = {8, 10, 12};
        for (int rank = 1; rank <= 3; rank++) {
            String path = "/data/rpg_project/rpg_skills/wizard_magic_lighthouse_rank_" + rank + ".json";
            try (var stream = MagicLighthouseProductionDefinitionTest.class.getResourceAsStream(path)) {
                assertNotNull(stream, "Missing production skill resource " + path);
                try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    SkillDefinition skill = SkillRegistry.parse(
                            new ResourceLocation("rpg_project", "wizard_magic_lighthouse"),
                            JsonParser.parseReader(reader).getAsJsonObject());
                    assertEquals(rank, skill.rank());
                    assertEquals(mp[rank - 1], skill.resourceCost());
                    assertEquals(cooldown[rank - 1], skill.cooldownTicks());
                    assertEquals(SkillTargetingType.SELF_AOE, skill.targeting());
                    assertEquals(MovementPolicy.LOCKED, skill.movementPolicy());
                    assertEquals(1, skill.hits().size());
                    var beacon = skill.hits().get(0).tauntBeacon();
                    assertTrue(beacon.active());
                    assertEquals(duration[rank - 1], beacon.durationTicks());
                    assertEquals(radius[rank - 1], beacon.radius());
                    assertEquals(10, beacon.refreshIntervalTicks());
                    assertEquals(10, beacon.maxTargets());
                    assertEquals(2, beacon.placementDistance());
                    assertEquals(Set.of(MobControlProfile.NORMAL, MobControlProfile.ELITE),
                            beacon.allowedProfiles());
                }
            }
        }
    }

    @Test
    void activeBeaconRejectsIncompleteRuntimeValues() {
        assertThrows(IllegalArgumentException.class, () ->
                new SkillDefinition.TauntBeaconPayload(100, 0, 8, 10, 2,
                        Set.of(MobControlProfile.NORMAL)));
    }
}
