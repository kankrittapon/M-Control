package com.zexqm.rpgproject.rpg.combat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.zexqm.rpgproject.RpgProject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public final class CombatConfig extends SimpleJsonResourceReloadListener {
    private static volatile Values current = Values.defaults();

    public CombatConfig() {
        super(new com.google.gson.Gson(), "rpg_world_core");
    }

    public static Values values() {
        return current;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resources, ProfilerFiller profiler) {
        JsonElement element = entries.get(new ResourceLocation(RpgProject.MOD_ID, "combat"));
        if (element == null) {
            RpgProject.LOGGER.warn("Missing rpg_world_core/combat.json; using built-in combat defaults");
            current = Values.defaults();
            return;
        }

        try {
            current = Values.parse(GsonHelper.convertToJsonObject(element, "combat"));
            RpgProject.LOGGER.info("Loaded RPG World Core combat configuration");
        } catch (RuntimeException exception) {
            RpgProject.LOGGER.error("Invalid rpg_world_core/combat.json; retaining the last valid values", exception);
        }
    }

    public record Values(double powerScale, double minimumHitChance, double maximumHitChance,
                         double maximumDamageReduction, double frontalGuardArcDegrees,
                         double rearAttackArcDegrees, double guardDamageScale, double guardRegenPerTick,
                         double maximumCcPoints, int ccImmunityTicks, int ccChainResetTicks,
                         double stiffnessPoints, double standardCcPoints,
                         int stiffnessTicks, int stunTicks, int knockdownTicks, int floatingTicks,
                         int boundTicks, int knockbackTicks, int freezingTicks,
                         double floatingVelocity, double knockbackVelocity,
                         double backAttackMultiplier, double downAttackMultiplier,
                         double airAttackMultiplier, double speedAttackMultiplier,
                         double counterAttackMultiplier) {
        public static Values defaults() {
            return new Values(0.10, 0.10, 0.95, 0.80, 180.0, 120.0, 1.0, 0.5,
                    2.0, 100, 100, 0.7, 1.0, 20, 40, 40, 20, 40, 10, 40,
                    0.8, 0.6, 1.2, 1.2, 1.7, 1.2, 1.7);
        }

        static Values parse(JsonObject root) {
            Values defaults = defaults();
            JsonObject damage = object(root, "damage");
            JsonObject protection = object(root, "protection");
            JsonObject cc = object(root, "crowd_control");
            JsonObject durations = object(cc, "durations");
            JsonObject special = object(root, "special_damage");
            Values parsed = new Values(
                    number(damage, "power_scale", defaults.powerScale),
                    number(damage, "minimum_hit_chance", defaults.minimumHitChance),
                    number(damage, "maximum_hit_chance", defaults.maximumHitChance),
                    number(damage, "maximum_damage_reduction", defaults.maximumDamageReduction),
                    number(protection, "frontal_guard_arc_degrees", defaults.frontalGuardArcDegrees),
                    number(special, "rear_attack_arc_degrees", defaults.rearAttackArcDegrees),
                    number(protection, "guard_damage_scale", defaults.guardDamageScale),
                    number(protection, "guard_regen_per_tick", defaults.guardRegenPerTick),
                    number(cc, "maximum_points", defaults.maximumCcPoints),
                    integer(cc, "immunity_ticks", defaults.ccImmunityTicks),
                    integer(cc, "chain_reset_ticks", defaults.ccChainResetTicks),
                    number(cc, "stiffness_points", defaults.stiffnessPoints),
                    number(cc, "standard_points", defaults.standardCcPoints),
                    integer(durations, "stiffness", defaults.stiffnessTicks),
                    integer(durations, "stun", defaults.stunTicks),
                    integer(durations, "knockdown", defaults.knockdownTicks),
                    integer(durations, "floating", defaults.floatingTicks),
                    integer(durations, "bound", defaults.boundTicks),
                    integer(durations, "knockback", defaults.knockbackTicks),
                    integer(durations, "freezing", defaults.freezingTicks),
                    number(cc, "floating_velocity", defaults.floatingVelocity),
                    number(cc, "knockback_velocity", defaults.knockbackVelocity),
                    number(special, "back_attack", defaults.backAttackMultiplier),
                    number(special, "down_attack", defaults.downAttackMultiplier),
                    number(special, "air_attack", defaults.airAttackMultiplier),
                    number(special, "speed_attack", defaults.speedAttackMultiplier),
                    number(special, "counter_attack", defaults.counterAttackMultiplier));
            parsed.validate();
            return parsed;
        }

        private void validate() {
            if (powerScale < 0 || minimumHitChance < 0 || maximumHitChance > 1
                    || minimumHitChance > maximumHitChance || maximumDamageReduction < 0
                    || maximumDamageReduction > 1 || frontalGuardArcDegrees <= 0 || frontalGuardArcDegrees > 360
                    || rearAttackArcDegrees <= 0 || rearAttackArcDegrees > 360 || maximumCcPoints <= 0
                    || ccImmunityTicks < 0 || ccChainResetTicks < 0 || stiffnessPoints <= 0
                    || standardCcPoints <= 0 || stiffnessTicks < 0 || stunTicks < 0 || knockdownTicks < 0
                    || floatingTicks < 0 || boundTicks < 0 || knockbackTicks < 0 || freezingTicks < 0
                    || floatingVelocity < 0 || knockbackVelocity < 0 || backAttackMultiplier < 0
                    || downAttackMultiplier < 0 || airAttackMultiplier < 0 || speedAttackMultiplier < 0
                    || counterAttackMultiplier < 0) {
                throw new JsonParseException("Combat configuration contains an out-of-range value");
            }
        }

        private static JsonObject object(JsonObject parent, String key) {
            return parent.has(key) ? GsonHelper.getAsJsonObject(parent, key) : new JsonObject();
        }

        private static double number(JsonObject object, String key, double fallback) {
            return object.has(key) ? GsonHelper.getAsDouble(object, key) : fallback;
        }

        private static int integer(JsonObject object, String key, int fallback) {
            return object.has(key) ? GsonHelper.getAsInt(object, key) : fallback;
        }
    }
}
