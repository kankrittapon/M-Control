package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.rpg.*;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import com.zexqm.rpgproject.rpg.combat.SmashType;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import com.zexqm.rpgproject.rpg.status.StatusStackingPolicy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public final class SkillRegistry extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, NavigableMap<Integer, SkillDefinition>> definitions = Map.of();

    public SkillRegistry() { super(new com.google.gson.Gson(), "rpg_skills"); }
    public static SkillDefinition get(ResourceLocation id) { return get(id, 1); }
    public static SkillDefinition get(ResourceLocation id, int rank) {
        NavigableMap<Integer, SkillDefinition> ranks = definitions.get(id);
        return ranks == null ? null : ranks.get(rank);
    }
    public static Collection<SkillDefinition> all() {
        return definitions.values().stream().flatMap(ranks -> ranks.values().stream()).toList();
    }
    static void replaceForTests(Map<ResourceLocation, SkillDefinition> values) {
        Map<ResourceLocation, NavigableMap<Integer, SkillDefinition>> grouped = new HashMap<>();
        values.values().forEach(definition -> grouped.computeIfAbsent(definition.id(), ignored -> new TreeMap<>())
                .put(definition.rank(), definition));
        definitions = immutable(grouped);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, NavigableMap<Integer, SkillDefinition>> loaded = new HashMap<>();
        entries.forEach((resourceId, element) -> {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(element, "skill");
                ResourceLocation skillId = root.has("skill_id")
                        ? new ResourceLocation(GsonHelper.getAsString(root, "skill_id")) : resourceId;
                SkillDefinition definition = parse(skillId, root);
                SkillDefinition previous = loaded.computeIfAbsent(skillId, ignored -> new TreeMap<>())
                        .putIfAbsent(definition.rank(), definition);
                if (previous != null) throw new IllegalArgumentException(
                        "Duplicate skill rank " + skillId + " rank=" + definition.rank());
            } catch (RuntimeException exception) {
                RpgProject.LOGGER.error("Rejected invalid skill definition {}", resourceId, exception);
            }
        });
        definitions = immutable(loaded);
        RpgProject.LOGGER.info("Loaded {} RPG skill definitions across {} stable skill IDs", all().size(), definitions.size());
    }

    private static Map<ResourceLocation, NavigableMap<Integer, SkillDefinition>> immutable(
            Map<ResourceLocation, NavigableMap<Integer, SkillDefinition>> values) {
        Map<ResourceLocation, NavigableMap<Integer, SkillDefinition>> result = new HashMap<>();
        values.forEach((id, ranks) -> result.put(id, Collections.unmodifiableNavigableMap(new TreeMap<>(ranks))));
        return Map.copyOf(result);
    }

    static SkillDefinition parse(ResourceLocation id, JsonObject root) {
        boolean debug = GsonHelper.getAsBoolean(root, "debug_only", false);
        boolean innate = GsonHelper.getAsBoolean(root, "innate", false);
        RpgClass rpgClass = value(RpgClass.class, root, "class", debug ? RpgClass.WIZARD : null);
        Specialization specialization = value(Specialization.class, root, "specialization", null);
        JsonObject weapon = object(root, "weapons");
        SkillWeaponRequirement weapons = new SkillWeaponRequirement(
                value(WeaponSet.class, weapon, "weapon_set", WeaponSet.MAIN),
                GsonHelper.getAsBoolean(weapon, "main", true),
                GsonHelper.getAsBoolean(weapon, "sub", false),
                GsonHelper.getAsBoolean(weapon, "awakening", false));
        double targetPenalty = GsonHelper.getAsDouble(root, "additional_target_damage_penalty", 0);
        double minimumTargetMultiplier = GsonHelper.getAsDouble(root, "minimum_target_damage_multiplier", 1);
        List<SkillDefinition.Hit> hits = new ArrayList<>();
        for (JsonElement hitElement : array(root, "hits")) hits.add(parseHit(
                hitElement.getAsJsonObject(), targetPenalty, minimumTargetMultiplier));
        List<SkillDefinition.ProtectionWindow> windows = new ArrayList<>();
        for (JsonElement window : array(root, "protection_windows")) {
            JsonObject item = window.getAsJsonObject();
            windows.add(new SkillDefinition.ProtectionWindow(value(ProtectionType.class, item, "type", null),
                    GsonHelper.getAsInt(item, "from_tick"), GsonHelper.getAsInt(item, "to_tick")));
        }
        JsonObject recast = object(root, "cooldown_recast");
        boolean recastEnabled = GsonHelper.getAsBoolean(recast, "enabled", false);
        SkillDefinition.CooldownRecastPolicy cooldownRecast = recastEnabled
                ? new SkillDefinition.CooldownRecastPolicy(true,
                GsonHelper.getAsDouble(recast, "damage_multiplier"),
                GsonHelper.getAsBoolean(recast, "allow_critical", false),
                GsonHelper.getAsBoolean(recast, "allow_special_attacks", false),
                GsonHelper.getAsBoolean(recast, "allow_cc", false),
                GsonHelper.getAsBoolean(recast, "allow_smash", false),
                GsonHelper.getAsBoolean(recast, "allow_statuses", false),
                GsonHelper.getAsBoolean(recast, "allow_resources", false),
                GsonHelper.getAsBoolean(recast, "allow_protection", false))
                : SkillDefinition.CooldownRecastPolicy.DISABLED;
        JsonObject links = object(root, "links");
        ResourceLocation grants = links.has("grant")
                ? new ResourceLocation(GsonHelper.getAsString(links, "grant")) : null;
        ResourceLocation requires = links.has("requires")
                ? new ResourceLocation(GsonHelper.getAsString(links, "requires")) : null;
        SkillDefinition.SkillLinkPolicy linkPolicy = new SkillDefinition.SkillLinkPolicy(
                grants, GsonHelper.getAsInt(links, "grant_duration_ticks", 0),
                value(SkillLinkTiming.class, links, "grant_timing", SkillLinkTiming.CAST_START),
                requires, GsonHelper.getAsBoolean(links, "consume_required_on_cast_start", false),
                GsonHelper.getAsBoolean(links, "use_required_anchor", false));
        CancelPolicy cancelPolicy = value(CancelPolicy.class, root, "cancel_policy", CancelPolicy.NEVER);
        JsonObject transitions = object(root, "transitions");
        SkillDefinition.TransitionPolicy transitionPolicy = transitions.size() == 0
                ? SkillDefinition.TransitionPolicy.fromLegacy(cancelPolicy)
                : new SkillDefinition.TransitionPolicy(
                GsonHelper.getAsInt(transitions, "movement_cancel_from_tick", -1),
                GsonHelper.getAsBoolean(transitions, "movement_until_first_hit", false),
                GsonHelper.getAsInt(transitions, "skill_cancel_from_tick", -1),
                GsonHelper.getAsBoolean(transitions, "skill_until_first_hit", false),
                GsonHelper.getAsBoolean(transitions, "interrupts_casting", false));
        return new SkillDefinition(id, debug, innate, rpgClass, specialization,
                GsonHelper.getAsInt(root, "rank", 1), value(SkillTargetingType.class, root, "targeting", null),
                weapons, value(PrimaryResourceType.class, root, "resource_type", PrimaryResourceType.MP),
                GsonHelper.getAsInt(root, "resource_cost", 0), GsonHelper.getAsDouble(root, "stamina_cost", 0),
                GsonHelper.getAsInt(root, "cooldown_ticks", 0), GsonHelper.getAsInt(root, "cast_ticks", 0),
                GsonHelper.getAsInt(root, "recovery_ticks", 0),
                value(MovementPolicy.class, root, "movement_policy", MovementPolicy.LOCKED),
                cancelPolicy,
                GsonHelper.getAsDouble(root, "range", 0), GsonHelper.getAsDouble(root, "radius", 0),
                hits, windows, cooldownRecast, linkPolicy, transitionPolicy,
                GsonHelper.getAsDouble(root, "projectile_speed", 0),
                value(FacingPolicy.class, root, "facing_policy", FacingPolicy.NONE),
                GsonHelper.getAsDouble(root, "turn_speed", 0),
                GsonHelper.getAsDouble(root, "cast_mp_recovery_percent", 0),
                value(CasterMovementType.class, root, "caster_movement_type", CasterMovementType.NONE),
                GsonHelper.getAsDouble(root, "caster_lateral_distance", 0));
    }

    private static SkillDefinition.Hit parseHit(JsonObject root, double defaultTargetPenalty,
                                                double defaultMinimumTargetMultiplier) {
        Set<SpecialAttackType> specials = EnumSet.noneOf(SpecialAttackType.class);
        for (JsonElement element : array(root, "special_attacks"))
            specials.add(SpecialAttackType.valueOf(element.getAsString().toUpperCase(Locale.ROOT)));
        List<SkillDefinition.StatusPayload> statuses = new ArrayList<>();
        for (JsonElement element : array(root, "statuses")) {
            JsonObject status = element.getAsJsonObject();
            Set<com.zexqm.rpgproject.rpg.mob.MobControlProfile> profiles =
                    EnumSet.noneOf(com.zexqm.rpgproject.rpg.mob.MobControlProfile.class);
            for (JsonElement profile : array(status, "allowed_profiles"))
                profiles.add(com.zexqm.rpgproject.rpg.mob.MobControlProfile.valueOf(
                        profile.getAsString().toUpperCase(Locale.ROOT)));
            statuses.add(new SkillDefinition.StatusPayload(value(RpgStatusType.class, status, "type", null),
                    GsonHelper.getAsInt(status, "duration_ticks"), GsonHelper.getAsInt(status, "interval_ticks", 20),
                    GsonHelper.getAsDouble(status, "potency"), GsonHelper.getAsInt(status, "max_stacks", 1),
                    value(StatusStackingPolicy.class, status, "stacking", StatusStackingPolicy.REFRESH), profiles));
        }
        List<SkillDefinition.SmashPayload> smashes = new ArrayList<>();
        for (JsonElement element : array(root, "smashes")) {
            JsonObject smash = element.getAsJsonObject();
            smashes.add(new SkillDefinition.SmashPayload(value(SmashType.class, smash, "type", null),
                    GsonHelper.getAsDouble(smash, "chance", 1.0)));
        }
        JsonObject resource = object(root, "resources");
        SkillDefinition.ResourcePayload resources = new SkillDefinition.ResourcePayload(
                GsonHelper.getAsDouble(resource, "max_mp_recovery_percent", 0),
                GsonHelper.getAsInt(resource, "flat_mp_recovery", 0),
                GsonHelper.getAsDouble(resource, "target_max_resource_drain_percent", 0),
                GsonHelper.getAsDouble(resource, "drain_transfer_ratio", 0),
                GsonHelper.getAsBoolean(resource, "recover_once_per_hit_window", true));
        JsonObject health = object(root, "health");
        double selfHealthPercent = GsonHelper.getAsDouble(health, "max_health_recovery_percent", 0);
        int selfFlatHealth = GsonHelper.getAsInt(health, "flat_health_recovery", 0);
        SkillDefinition.HealthPayload healthPayload = new SkillDefinition.HealthPayload(
                selfHealthPercent, selfFlatHealth,
                GsonHelper.getAsDouble(health, "ally_max_health_recovery_percent", selfHealthPercent),
                GsonHelper.getAsInt(health, "ally_flat_health_recovery", selfFlatHealth));
        JsonObject defensive = object(root, "defensive");
        SkillDefinition.DefensivePayload defensivePayload = new SkillDefinition.DefensivePayload(
                GsonHelper.getAsInt(defensive, "mana_shield_ticks", 0),
                GsonHelper.getAsDouble(defensive, "mana_shield_ratio", 0),
                GsonHelper.getAsInt(defensive, "resistance_ticks", 0),
                GsonHelper.getAsDouble(defensive, "resistance_bonus", 0),
                GsonHelper.getAsInt(defensive, "damage_reduction_ticks", 0),
                GsonHelper.getAsDouble(defensive, "damage_reduction_ratio", 0));
        return new SkillDefinition.Hit(GsonHelper.getAsInt(root, "timing_tick"),
                GsonHelper.getAsDouble(root, "base_damage", 0), GsonHelper.getAsDouble(root, "coefficient", 1),
                GsonHelper.getAsDouble(root, "radius", 0), value(RpgPowerType.class, root, "power_type", RpgPowerType.NONE),
                value(CrowdControlType.class, root, "crowd_control", null),
                value(CrowdControlType.class, root, "player_crowd_control", null),
                GsonHelper.getAsDouble(root, "pull_strength", 0), specials, statuses, smashes, resources,
                value(SkillImpactShape.class, root, "impact_shape", SkillImpactShape.AUTO),
                GsonHelper.getAsInt(root, "max_targets", 0),
                GsonHelper.getAsDouble(root, "forward_offset", 0),
                GsonHelper.getAsDouble(root, "right_offset", 0),
                GsonHelper.getAsDouble(root, "hit_chance_bonus", 0),
                GsonHelper.getAsDouble(root, "critical_chance_bonus", 0),
                GsonHelper.getAsDouble(root, "additional_target_damage_penalty", defaultTargetPenalty),
                GsonHelper.getAsDouble(root, "minimum_target_damage_multiplier", defaultMinimumTargetMultiplier),
                value(SkillTargetDisposition.class, root, "target_disposition", SkillTargetDisposition.HOSTILE),
                healthPayload, defensivePayload);
    }

    private static JsonObject object(JsonObject root, String key) {
        return root.has(key) ? GsonHelper.getAsJsonObject(root, key) : new JsonObject();
    }

    private static JsonArray array(JsonObject root, String key) {
        return root.has(key) ? GsonHelper.getAsJsonArray(root, key) : new JsonArray();
    }

    private static <E extends Enum<E>> E value(Class<E> type, JsonObject root, String key, E fallback) {
        if (!root.has(key)) {
            if (fallback == null && (key.equals("class") || key.equals("targeting") || key.equals("type")))
                throw new IllegalArgumentException("Missing " + key);
            return fallback;
        }
        return Enum.valueOf(type, GsonHelper.getAsString(root, key).toUpperCase(Locale.ROOT));
    }
}
