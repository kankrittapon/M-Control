package com.zexqm.rpgproject.rpg;

import net.minecraft.nbt.CompoundTag;

public final class TrainingProgress {
    public static final int MAX_LEVEL = 50;
    private int level = 1;
    private long experience;

    public int level() { return level; }
    public long experience() { return experience; }
    public long requiredExperience() { return level >= MAX_LEVEL ? 0 : 250L * level * level; }

    public int addExperience(long amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        experience += amount;
        int gained = 0;
        while (level < MAX_LEVEL && experience >= requiredExperience()) {
            experience -= requiredExperience();
            level++;
            gained++;
        }
        if (level >= MAX_LEVEL) experience = 0;
        return gained;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", level); tag.putLong("Experience", experience);
        return tag;
    }
    public void load(CompoundTag tag) {
        level = Math.max(1, Math.min(MAX_LEVEL, tag.getInt("Level")));
        experience = Math.max(0, tag.getLong("Experience"));
    }
}
