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

public final class SkillRuntimeConfig extends SimpleJsonResourceReloadListener {
    private static volatile Values current = Values.defaults();

    public SkillRuntimeConfig() { super(new com.google.gson.Gson(), "rpg_world_core"); }
    public static Values values() { return current; }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        JsonElement element = entries.get(new ResourceLocation(RpgProject.MOD_ID, "skill_runtime"));
        if (element == null) return;
        try {
            JsonObject root = GsonHelper.convertToJsonObject(element, "skill_runtime");
            int draw = GsonHelper.getAsInt(root, "draw_ticks", 8);
            int sheathe = GsonHelper.getAsInt(root, "sheathe_ticks", 6);
            if (draw < 1 || sheathe < 1) throw new IllegalArgumentException("Transition ticks must be positive");
            JsonObject damage = GsonHelper.getAsJsonObject(root, "bdo_damage_conversion", new JsonObject());
            BdoDamageValues bdoDamage = new BdoDamageValues(
                    GsonHelper.getAsDouble(damage, "reference_percent", 1000.0),
                    GsonHelper.getAsDouble(damage, "exponent", 0.5),
                    GsonHelper.getAsDouble(damage, "multiplier", 1.0),
                    GsonHelper.getAsDouble(damage, "min_per_hit_coefficient", 0.1),
                    GsonHelper.getAsDouble(damage, "max_per_hit_coefficient", 5.0),
                    GsonHelper.getAsDouble(damage, "max_total_coefficient", 20.0),
                    GsonHelper.getAsInt(damage, "max_source_hits", 100));
            JsonObject observability = GsonHelper.getAsJsonObject(root, "observability", new JsonObject());
            ObservabilityValues diagnostics = new ObservabilityValues(
                    GsonHelper.getAsBoolean(observability, "log_hit_performance", true),
                    GsonHelper.getAsBoolean(observability, "log_per_target_results", true),
                    GsonHelper.getAsLong(observability, "slow_resolver_micros", 1000));
            current = new Values(draw, sheathe, bdoDamage, diagnostics);
        } catch (RuntimeException exception) {
            RpgProject.LOGGER.error("Invalid skill_runtime.json; retaining last valid values", exception);
        }
    }

    public record Values(int drawTicks, int sheatheTicks, BdoDamageValues bdoDamage,
                         ObservabilityValues observability) {
        public Values {
            if (drawTicks < 1 || sheatheTicks < 1 || bdoDamage == null || observability == null)
                throw new IllegalArgumentException("Invalid skill runtime values");
        }

        public static Values defaults() {
            return new Values(8, 6, BdoDamageValues.defaults(), ObservabilityValues.defaults());
        }
    }

    public record ObservabilityValues(boolean logHitPerformance, boolean logPerTargetResults,
                                      long slowResolverMicros) {
        public ObservabilityValues {
            if (slowResolverMicros < 0) throw new IllegalArgumentException("Invalid resolver threshold");
        }

        public static ObservabilityValues defaults() { return new ObservabilityValues(true, true, 1000); }
    }

    public record BdoDamageValues(double referencePercent, double exponent, double multiplier,
                                  double minPerHitCoefficient, double maxPerHitCoefficient,
                                  double maxTotalCoefficient, int maxSourceHits) {
        public BdoDamageValues {
            if (!positive(referencePercent) || !positive(exponent) || !positive(multiplier)
                    || !positive(minPerHitCoefficient) || !positive(maxPerHitCoefficient)
                    || !positive(maxTotalCoefficient) || maxSourceHits < 1
                    || minPerHitCoefficient > maxPerHitCoefficient
                    || maxPerHitCoefficient > maxTotalCoefficient)
                throw new IllegalArgumentException("Invalid BDO damage conversion values");
        }

        private static boolean positive(double value) { return Double.isFinite(value) && value > 0; }
        public static BdoDamageValues defaults() {
            return new BdoDamageValues(1000.0, 0.5, 1.0, 0.1, 5.0, 20.0, 100);
        }
    }
}
