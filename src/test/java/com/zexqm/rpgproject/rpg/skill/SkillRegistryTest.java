package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import com.zexqm.rpgproject.rpg.combat.SmashType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillRegistryTest {
    @Test
    void parsesMultiHitProtectionAndStatusProfile() {
        String json = """
                {"debug_only":true,"targeting":"cone","range":6,
                 "hits":[{"timing_tick":2,"statuses":[{"type":"burn","duration_ticks":40,
                   "potency":1,"allowed_profiles":["normal","boss"]}],
                   "smashes":[{"type":"down_smash","chance":0.15}]}],
                 "protection_windows":[{"type":"super_armor","from_tick":0,"to_tick":2}]}
                """;
        SkillDefinition skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "test"),
                JsonParser.parseString(json).getAsJsonObject());
        assertEquals(SkillTargetingType.CONE, skill.targeting());
        assertEquals(1, skill.hits().size());
        assertTrue(skill.hits().get(0).statuses().get(0).allowedProfiles().contains(MobControlProfile.BOSS));
        assertEquals(SmashType.DOWN_SMASH, skill.hits().get(0).smashes().get(0).type());
        assertEquals(0.15, skill.hits().get(0).smashes().get(0).chance());
        assertTrue(skill.protectionWindows().get(0).active(2));
    }

    @Test
    void rejectsNegativeTiming() {
        String json = "{\"debug_only\":true,\"targeting\":\"ray\",\"hits\":[{\"timing_tick\":-1}]}";
        assertThrows(IllegalArgumentException.class, () -> SkillRegistry.parse(
                new ResourceLocation("rpg_project", "bad"), JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    void parsesSafeCooldownRecastDefaults() {
        String json = """
                {"debug_only":true,"targeting":"ray",
                 "cooldown_recast":{"enabled":true,"damage_multiplier":0.35}}
                """;
        SkillDefinition skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "recast"),
                JsonParser.parseString(json).getAsJsonObject());
        assertTrue(skill.cooldownRecast().enabled());
        assertEquals(0.35, skill.cooldownRecast().damageMultiplier());
        assertFalse(skill.cooldownRecast().allowCritical());
        assertFalse(skill.cooldownRecast().allowCrowdControl());
        assertFalse(skill.cooldownRecast().allowProtection());
    }

    @Test
    void rejectsEnabledCooldownRecastWithoutDamage() {
        String json = """
                {"debug_only":true,"targeting":"ray",
                 "cooldown_recast":{"enabled":true,"damage_multiplier":0}}
                """;
        assertThrows(IllegalArgumentException.class, () -> SkillRegistry.parse(
                new ResourceLocation("rpg_project", "bad_recast"),
                JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    void parsesSkillLinkContract() {
        String json = """
                {"debug_only":true,"targeting":"ray","links":{
                 "grant":"rpg_project:fireball_ready","grant_duration_ticks":140,
                 "grant_timing":"cast_start","requires":"rpg_project:fireball_primed",
                 "consume_required_on_cast_start":true,"use_required_anchor":true}}
                """;
        SkillDefinition skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "link"),
                JsonParser.parseString(json).getAsJsonObject());
        assertEquals(new ResourceLocation("rpg_project", "fireball_ready"), skill.links().grants());
        assertEquals(140, skill.links().grantDurationTicks());
        assertEquals(SkillLinkTiming.CAST_START, skill.links().grantTiming());
        assertTrue(skill.links().consumeRequiredOnCastStart());
        assertTrue(skill.links().useRequiredAnchor());
    }

    @Test
    void groupsRankFilesUnderStableSkillId() {
        ResourceLocation stableId = new ResourceLocation("rpg_project", "wizard_fireball");
        SkillDefinition rankTwo = SkillRegistry.parse(stableId, JsonParser.parseString(
                "{\"debug_only\":true,\"rank\":2,\"targeting\":\"ray\"}").getAsJsonObject());
        SkillRegistry.replaceForTests(java.util.Map.of(
                new ResourceLocation("rpg_project", "wizard_fireball_rank_2"), rankTwo));
        assertNull(SkillRegistry.get(stableId, 1));
        assertSame(rankTwo, SkillRegistry.get(stableId, 2));
    }

    @Test
    void parsesTravelingProjectileContract() {
        String json = """
                {"debug_only":true,"targeting":"aim_projectile","range":24,
                 "projectile_speed":1.25,"hits":[{"timing_tick":8}]}
                """;
        SkillDefinition skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "fireball"),
                JsonParser.parseString(json).getAsJsonObject());
        assertEquals(1.25, skill.projectileSpeed());
        assertEquals(24.0, skill.range());
    }

    @Test
    void parsesPerHitAreaAndMeteorOffsetContract() {
        String json = """
                {"debug_only":true,"targeting":"ground_aoe","range":30,
                 "hits":[{"timing_tick":12,"impact_shape":"circle","radius":3.5,
                   "max_targets":8,"forward_offset":4,"right_offset":-2}]}
                """;
        SkillDefinition.Hit hit = SkillRegistry.parse(new ResourceLocation("rpg_project", "meteor"),
                JsonParser.parseString(json).getAsJsonObject()).hits().get(0);
        assertEquals(SkillImpactShape.CIRCLE, hit.impactShape());
        assertEquals(3.5, hit.radius());
        assertEquals(8, hit.maxTargets());
        assertEquals(4.0, hit.forwardOffset());
        assertEquals(-2.0, hit.rightOffset());
    }

    @Test
    void parsesCancelWindowAndInterruptingMovementSkill() {
        String json = """
                {"debug_only":true,"targeting":"movement","transitions":{
                 "movement_cancel_from_tick":2,"movement_until_first_hit":true,
                 "skill_cancel_from_tick":4,"skill_until_first_hit":false,
                 "interrupts_casting":true},"hits":[{"timing_tick":8}]}
                """;
        SkillDefinition skill = SkillRegistry.parse(new ResourceLocation("rpg_project", "blink"),
                JsonParser.parseString(json).getAsJsonObject());
        assertFalse(skill.transitions().allows(SkillCancelTrigger.MOVEMENT, 1, 8));
        assertTrue(skill.transitions().allows(SkillCancelTrigger.MOVEMENT, 2, 8));
        assertFalse(skill.transitions().allows(SkillCancelTrigger.MOVEMENT, 8, 8));
        assertTrue(skill.transitions().allows(SkillCancelTrigger.SKILL, 9, 8));
        assertTrue(skill.transitions().interruptsCasting());
    }

    @Test
    void rejectsProjectileWithoutPositiveSpeed() {
        String json = "{\"debug_only\":true,\"targeting\":\"aim_projectile\",\"range\":24}";
        assertThrows(IllegalArgumentException.class, () -> SkillRegistry.parse(
                new ResourceLocation("rpg_project", "bad_projectile"),
                JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    void parsesPerHitAccuracyAndCriticalBonuses() {
        String json = """
                {"debug_only":true,"targeting":"ray","hits":[{"timing_tick":1,
                 "hit_chance_bonus":0.0625,"critical_chance_bonus":0.4}]}
                """;
        SkillDefinition.Hit hit = SkillRegistry.parse(new ResourceLocation("rpg_project", "modifiers"),
                JsonParser.parseString(json).getAsJsonObject()).hits().get(0);
        assertEquals(0.0625, hit.hitChanceBonus());
        assertEquals(0.4, hit.criticalChanceBonus());
    }
}
