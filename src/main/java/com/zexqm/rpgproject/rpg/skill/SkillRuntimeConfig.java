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
    private static volatile Values current = new Values(8, 6);

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
            current = new Values(draw, sheathe);
        } catch (RuntimeException exception) {
            RpgProject.LOGGER.error("Invalid skill_runtime.json; retaining last valid values", exception);
        }
    }

    public record Values(int drawTicks, int sheatheTicks) {}
}
