package com.zexqm.rpgproject.rpg;

import com.google.gson.*;
import com.zexqm.rpgproject.RpgProject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import java.util.*;

public final class ClassProfileManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final EnumMap<RpgClass, ClassGrowthProfile> PROFILES = new EnumMap<>(RpgClass.class);

    public ClassProfileManager() { super(GSON, "rpg_classes"); }

    public static ClassGrowthProfile get(RpgClass cls) {
        return PROFILES.getOrDefault(cls, ClassGrowthProfile.defaults(cls));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        EnumMap<RpgClass, ClassGrowthProfile> loaded = new EnumMap<>(RpgClass.class);
        objects.forEach((id, element) -> {
            try {
                RpgClass cls = RpgClass.valueOf(id.getPath().toUpperCase(Locale.ROOT));
                loaded.put(cls, GSON.fromJson(element, ClassGrowthProfile.class));
            } catch (RuntimeException exception) {
                RpgProject.LOGGER.error("Invalid RPG class profile {}", id, exception);
            }
        });
        PROFILES.clear();
        PROFILES.putAll(loaded);
        RpgProject.LOGGER.info("Loaded {} RPG class profiles", PROFILES.size());
    }
}
