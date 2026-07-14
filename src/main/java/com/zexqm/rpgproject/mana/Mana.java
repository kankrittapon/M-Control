package com.zexqm.rpgproject.mana;

import net.minecraft.nbt.CompoundTag;

public class Mana {
    private int mana = 100;
    private int maxMana = 100;
    private int regenPerSecond = 5;
    private int regenTicks;

    public int getMana() {
        return mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMana(int value) {
        mana = Math.max(0, Math.min(value, maxMana));
    }

    public void setMaxMana(int value) {
        maxMana = Math.max(0, value);
        mana = Math.max(0, Math.min(mana, maxMana));
    }

    public int getRegenPerSecond() {
        return regenPerSecond;
    }

    public boolean spend(int amount) {
        if (mana < amount) {
            return false;
        }
        mana -= amount;
        return true;
    }

    public int restore(int amount) {
        if (amount <= 0) return 0;
        int before = mana;
        mana = Math.min(maxMana, mana + amount);
        return mana - before;
    }

    public int drain(int amount) {
        if (amount <= 0) return 0;
        int drained = Math.min(mana, amount);
        mana -= drained;
        return drained;
    }

    public void tickRegen() {
        regenTicks++;
        if (regenTicks >= 20) {
            regenTicks = 0;
            mana = Math.min(maxMana, mana + regenPerSecond);
        }
    }

    public void copyFrom(Mana other) {
        mana = other.mana;
        maxMana = other.maxMana;
        regenPerSecond = other.regenPerSecond;
        regenTicks = other.regenTicks;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mana", mana);
        tag.putInt("MaxMana", maxMana);
        tag.putInt("RegenPerSecond", regenPerSecond);
        tag.putInt("RegenTicks", regenTicks);
        return tag;
    }

    public void load(CompoundTag tag) {
        mana = tag.getInt("Mana");
        maxMana = Math.max(1, tag.getInt("MaxMana"));
        regenPerSecond = Math.max(0, tag.getInt("RegenPerSecond"));
        regenTicks = tag.getInt("RegenTicks");
        mana = Math.min(mana, maxMana);
    }
}
