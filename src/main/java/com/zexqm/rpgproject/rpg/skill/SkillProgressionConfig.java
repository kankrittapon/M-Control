package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zexqm.rpgproject.RpgProject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public final class SkillProgressionConfig extends SimpleJsonResourceReloadListener {
    private static final Values DEFAULTS = new Values(100, 2, 1000, 5, 2.0, 0.5);
    private static volatile Values current = DEFAULTS;

    public SkillProgressionConfig() { super(new com.google.gson.Gson(), "rpg_world_core"); }
    public static Values values() { return current; }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager,
                         ProfilerFiller profiler) {
        JsonElement element = entries.get(new ResourceLocation(RpgProject.MOD_ID, "skill_progression"));
        if (element == null) { current = DEFAULTS; return; }
        try {
            JsonObject root = GsonHelper.convertToJsonObject(element, "skill_progression");
            Values parsed = new Values(
                    GsonHelper.getAsInt(root, "base_xp_per_point", DEFAULTS.baseXpPerPoint),
                    GsonHelper.getAsInt(root, "xp_growth_per_point", DEFAULTS.xpGrowthPerPoint),
                    GsonHelper.getAsInt(root, "max_xp_per_point", DEFAULTS.maxXpPerPoint),
                    GsonHelper.getAsInt(root, "mob_reward_base", DEFAULTS.mobRewardBase),
                    GsonHelper.getAsDouble(root, "mob_level_multiplier", DEFAULTS.mobLevelMultiplier),
                    GsonHelper.getAsDouble(root, "mob_health_multiplier", DEFAULTS.mobHealthMultiplier));
            parsed.validate();
            current = parsed;
            RpgProject.LOGGER.info("Loaded RPG Skill Point progression configuration");
        } catch (RuntimeException exception) {
            RpgProject.LOGGER.error("Invalid skill_progression.json; retaining last valid values", exception);
        }
    }

    public record Values(int baseXpPerPoint, int xpGrowthPerPoint, int maxXpPerPoint,
                         int mobRewardBase, double mobLevelMultiplier, double mobHealthMultiplier) {
        void validate() {
            if (baseXpPerPoint < 1 || xpGrowthPerPoint < 0 || maxXpPerPoint < baseXpPerPoint
                    || mobRewardBase < 0 || mobLevelMultiplier < 0 || mobHealthMultiplier < 0)
                throw new IllegalArgumentException("Invalid Skill Point progression values");
        }

        public long requiredXp(int totalPoints) {
            return Math.min(maxXpPerPoint, (long) baseXpPerPoint
                    + (long) Math.max(0, totalPoints) * xpGrowthPerPoint);
        }

        public long mobReward(int mobLevel, double maximumHealth) {
            return Math.max(1L, Math.round(mobRewardBase + Math.max(1, mobLevel) * mobLevelMultiplier
                    + Math.max(0.0, maximumHealth) * mobHealthMultiplier));
        }
    }
}
