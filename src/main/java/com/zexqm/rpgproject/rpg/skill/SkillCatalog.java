package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.*;
import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.rpg.RpgClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public final class SkillCatalog extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, SkillCatalogEntry> entries = Map.of();

    public SkillCatalog() { super(new Gson(), "rpg_skill_catalog"); }
    public static SkillCatalogEntry get(ResourceLocation id) { return entries.get(id); }
    public static Collection<SkillCatalogEntry> all() { return entries.values(); }
    static void replaceForTests(Map<ResourceLocation, SkillCatalogEntry> values) { entries = Map.copyOf(values); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, SkillCatalogEntry> parsed = new HashMap<>();
        resources.forEach((id, value) -> {
            try {
                SkillCatalogEntry entry = parse(id, value.getAsJsonObject());
                if (parsed.putIfAbsent(entry.id(), entry) != null)
                    throw new IllegalArgumentException("Duplicate skill ID " + entry.id());
            } catch (RuntimeException exception) {
                RpgProject.LOGGER.error("Rejected skill catalog entry {}", id, exception);
            }
        });
        Set<ResourceLocation> invalid = validateGraph(parsed);
        invalid.forEach(parsed::remove);
        entries = Map.copyOf(parsed);
        long playable = entries.values().stream().filter(SkillCatalogEntry::playable).count();
        RpgProject.LOGGER.info("Loaded {} RPG catalog entries ({} playable, {} metadata-only)",
                entries.size(), playable, entries.size() - playable);
    }

    static SkillCatalogEntry parse(ResourceLocation id, JsonObject root) {
        SkillCostTier costTier = root.has("sp_tier")
                ? SkillCostTier.valueOf(GsonHelper.getAsString(root, "sp_tier").toUpperCase(Locale.ROOT))
                : null;
        List<SkillRankDefinition> ranks = new ArrayList<>();
        for (JsonElement value : array(root, "ranks")) {
            JsonObject rank = value.getAsJsonObject();
            Integer skillPointCost = rank.has("sp_cost")
                    ? Integer.valueOf(GsonHelper.getAsInt(rank, "sp_cost"))
                    : SkillProgressionConfig.values().skillPointCost(
                    costTier, GsonHelper.getAsInt(rank, "rank"));
            ranks.add(new SkillRankDefinition(GsonHelper.getAsInt(rank, "rank"),
                    GsonHelper.getAsInt(rank, "required_level", 1),
                    skillPointCost));
        }
        List<SkillRequirement> prerequisites = new ArrayList<>();
        for (JsonElement value : array(root, "prerequisites")) {
            JsonObject requirement = value.getAsJsonObject();
            prerequisites.add(new SkillRequirement(new ResourceLocation(
                    GsonHelper.getAsString(requirement, "skill")),
                    GsonHelper.getAsInt(requirement, "minimum_rank", 1)));
        }
        Set<ResourceLocation> exclusive = new HashSet<>();
        for (JsonElement value : array(root, "mutually_exclusive"))
            exclusive.add(new ResourceLocation(value.getAsString()));
        String iconValue = GsonHelper.getAsString(root, "icon", "");
        boolean playable = GsonHelper.getAsBoolean(root, "playable", false);
        return new SkillCatalogEntry(id, GsonHelper.getAsString(root, "mcp_id"),
                GsonHelper.getAsString(root, "name"), GsonHelper.getAsString(root, "description", ""),
                enumValue(RpgClass.class, root, "class"), enumValue(SkillTree.class, root, "tree"),
                iconValue.isBlank() ? null : iconValue, playable,
                GsonHelper.getAsString(root, "unavailable_reason", playable ? "" : "Missing combat data"),
                ranks, prerequisites, exclusive);
    }

    static Set<ResourceLocation> validateGraph(Map<ResourceLocation, SkillCatalogEntry> values) {
        Set<ResourceLocation> invalid = new HashSet<>();
        Map<String, ResourceLocation> mcpIds = new HashMap<>();
        values.forEach((id, entry) -> {
            ResourceLocation previous = mcpIds.putIfAbsent(entry.mcpId(), id);
            if (previous != null) {
                RpgProject.LOGGER.error("Catalog skills {} and {} share MCP ID {}", previous, id, entry.mcpId());
                invalid.add(previous);
                invalid.add(id);
            }
            for (SkillRequirement requirement : entry.prerequisites()) {
                SkillCatalogEntry required = values.get(requirement.skillId());
                if (required == null || requirement.minimumRank() > required.maximumRank()) {
                    RpgProject.LOGGER.error("Catalog skill {} has invalid prerequisite {}", id, requirement);
                    invalid.add(id);
                }
            }
            for (ResourceLocation exclusive : entry.mutuallyExclusive()) {
                if (!values.containsKey(exclusive)) {
                    RpgProject.LOGGER.error("Catalog skill {} has missing exclusive skill {}", id, exclusive);
                    invalid.add(id);
                }
            }
        });
        Map<ResourceLocation, Integer> colors = new HashMap<>();
        for (ResourceLocation id : values.keySet()) detectCycle(id, values, colors, invalid);
        return invalid;
    }

    private static void detectCycle(ResourceLocation id, Map<ResourceLocation, SkillCatalogEntry> values,
                                    Map<ResourceLocation, Integer> colors, Set<ResourceLocation> invalid) {
        int color = colors.getOrDefault(id, 0);
        if (color == 2 || invalid.contains(id)) return;
        if (color == 1) { invalid.add(id); RpgProject.LOGGER.error("Catalog prerequisite cycle at {}", id); return; }
        colors.put(id, 1);
        SkillCatalogEntry entry = values.get(id);
        if (entry != null) for (SkillRequirement requirement : entry.prerequisites()) {
            detectCycle(requirement.skillId(), values, colors, invalid);
            if (invalid.contains(requirement.skillId())) invalid.add(id);
        }
        colors.put(id, 2);
    }

    private static JsonArray array(JsonObject root, String key) {
        return root.has(key) ? root.getAsJsonArray(key) : new JsonArray();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, JsonObject root, String key) {
        return Enum.valueOf(type, GsonHelper.getAsString(root, key).toUpperCase(Locale.ROOT));
    }
}
