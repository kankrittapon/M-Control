package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.rpg.*;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import com.zexqm.rpgproject.rpg.status.StatusStackingPolicy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public final class SkillRegistry extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, SkillDefinition> definitions = Map.of();

    public SkillRegistry() { super(new com.google.gson.Gson(), "rpg_skills"); }
    public static SkillDefinition get(ResourceLocation id) { return definitions.get(id); }
    public static Collection<SkillDefinition> all() { return definitions.values(); }
    static void replaceForTests(Map<ResourceLocation, SkillDefinition> values) { definitions = Map.copyOf(values); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, SkillDefinition> loaded = new HashMap<>();
        entries.forEach((id, element) -> {
            try {
                SkillDefinition definition = parse(id, GsonHelper.convertToJsonObject(element, "skill"));
                loaded.put(definition.id(), definition);
            } catch (RuntimeException exception) {
                RpgProject.LOGGER.error("Rejected invalid skill definition {}", id, exception);
            }
        });
        definitions = Map.copyOf(loaded);
        RpgProject.LOGGER.info("Loaded {} RPG skill definitions", definitions.size());
    }

    static SkillDefinition parse(ResourceLocation id, JsonObject root) {
        boolean debug = GsonHelper.getAsBoolean(root, "debug_only", false);
        RpgClass rpgClass = value(RpgClass.class, root, "class", debug ? RpgClass.WIZARD : null);
        Specialization specialization = value(Specialization.class, root, "specialization", null);
        JsonObject weapon = object(root, "weapons");
        SkillWeaponRequirement weapons = new SkillWeaponRequirement(
                value(WeaponSet.class, weapon, "weapon_set", WeaponSet.MAIN),
                GsonHelper.getAsBoolean(weapon, "main", true),
                GsonHelper.getAsBoolean(weapon, "sub", false),
                GsonHelper.getAsBoolean(weapon, "awakening", false));
        List<SkillDefinition.Hit> hits = new ArrayList<>();
        for (JsonElement hitElement : array(root, "hits")) hits.add(parseHit(hitElement.getAsJsonObject()));
        List<SkillDefinition.ProtectionWindow> windows = new ArrayList<>();
        for (JsonElement window : array(root, "protection_windows")) {
            JsonObject item = window.getAsJsonObject();
            windows.add(new SkillDefinition.ProtectionWindow(value(ProtectionType.class, item, "type", null),
                    GsonHelper.getAsInt(item, "from_tick"), GsonHelper.getAsInt(item, "to_tick")));
        }
        return new SkillDefinition(id, debug, rpgClass, specialization,
                GsonHelper.getAsInt(root, "rank", 1), value(SkillTargetingType.class, root, "targeting", null),
                weapons, value(PrimaryResourceType.class, root, "resource_type", PrimaryResourceType.MP),
                GsonHelper.getAsInt(root, "resource_cost", 0), GsonHelper.getAsDouble(root, "stamina_cost", 0),
                GsonHelper.getAsInt(root, "cooldown_ticks", 0), GsonHelper.getAsInt(root, "cast_ticks", 0),
                GsonHelper.getAsInt(root, "recovery_ticks", 0),
                value(MovementPolicy.class, root, "movement_policy", MovementPolicy.LOCKED),
                value(CancelPolicy.class, root, "cancel_policy", CancelPolicy.NEVER),
                GsonHelper.getAsDouble(root, "range", 0), GsonHelper.getAsDouble(root, "radius", 0), hits, windows);
    }

    private static SkillDefinition.Hit parseHit(JsonObject root) {
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
        return new SkillDefinition.Hit(GsonHelper.getAsInt(root, "timing_tick"),
                GsonHelper.getAsDouble(root, "base_damage", 0), GsonHelper.getAsDouble(root, "coefficient", 1),
                GsonHelper.getAsDouble(root, "radius", 0), value(RpgPowerType.class, root, "power_type", RpgPowerType.NONE),
                value(CrowdControlType.class, root, "crowd_control", null), specials, statuses);
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
