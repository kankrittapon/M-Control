package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class PlayerSkillProgress {
    private final Map<ResourceLocation, Integer> learnedRanks = new HashMap<>();

    public int rank(ResourceLocation skill) { return learnedRanks.getOrDefault(skill, 0); }
    public Map<ResourceLocation, Integer> learnedRanks() { return Map.copyOf(learnedRanks); }

    public void setRank(ResourceLocation skill, int rank) {
        if (rank <= 0) learnedRanks.remove(skill); else learnedRanks.put(skill, rank);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        learnedRanks.forEach((id, rank) -> tag.putInt(id.toString(), rank));
        return tag;
    }

    public void load(CompoundTag tag) {
        learnedRanks.clear();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            int rank = tag.getInt(key);
            if (id != null && rank > 0) learnedRanks.put(id, rank);
        }
    }
}
