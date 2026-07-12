package com.zexqm.rpgproject.rpg;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class RpgPlayerData {
    public static final int COMBAT_TIMEOUT_TICKS = 200;
    private RpgClass rpgClass = RpgClass.WIZARD;
    private Specialization specialization = Specialization.SUCCESSION;
    private WeaponSet activeSet = WeaponSet.MAIN;
    private final ItemStack[] weapons = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private boolean weaponDrawn;
    private int combatTicks;
    private int level = LevelCurve.MIN_LEVEL;
    private long experience;
    private int totalSkillPoints;
    private int spentSkillPoints;
    private final TrainingProgress breath = new TrainingProgress();
    private final TrainingProgress strength = new TrainingProgress();
    private final TrainingProgress healthTraining = new TrainingProgress();
    private double stamina = maxStamina();
    private int staminaRegenDelay;
    private double breathDistance;
    private double strengthDistance;
    private double healedHealth;
    private final ProtectionState protection = new ProtectionState();

    public RpgClass rpgClass() { return rpgClass; }
    public Specialization specialization() { return specialization; }
    public WeaponSet activeSet() { return activeSet; }
    public boolean weaponDrawn() { return weaponDrawn; }
    public boolean inCombat() { return weaponDrawn && combatTicks > 0; }
    public ItemStack weapon(WeaponSlot slot) { return weapons[slot.ordinal()]; }
    public int level() { return level; }
    public long experience() { return experience; }
    public long requiredExperience() { return LevelCurve.requiredExp(level); }
    public int totalSkillPoints() { return totalSkillPoints; }
    public int spentSkillPoints() { return spentSkillPoints; }
    public int availableSkillPoints() { return Math.max(0, totalSkillPoints - spentSkillPoints); }
    public DerivedStats stats() { return ClassProfileManager.get(rpgClass).atLevel(level); }
    public TrainingProgress breath() { return breath; }
    public TrainingProgress strength() { return strength; }
    public TrainingProgress healthTraining() { return healthTraining; }
    public double stamina() { return stamina; }
    public double maxStamina() { return 100.0 + breath.level() * 5.0; }
    public ProtectionState protection() { return protection; }
    public boolean exhausted() { return stamina < 1.0; }

    public void tickStamina(boolean sprinting) {
        double max = maxStamina();
        if (sprinting && stamina > 0) {
            stamina = Math.max(0, stamina - 0.75);
            staminaRegenDelay = 30;
        } else if (staminaRegenDelay > 0) {
            staminaRegenDelay--;
        } else {
            double regenPerTick = (12.0 + breath.level() * 0.15) / 20.0;
            stamina = Math.min(max, stamina + regenPerTick);
        }
        stamina = Math.min(stamina, max);
    }

    public boolean spendStamina(double amount) {
        if (amount <= 0 || stamina < amount) return false;
        stamina -= amount; staminaRegenDelay = 30; return true;
    }

    public void trainFromMovement(double distance, double loadRatio, boolean sprinting) {
        if (distance <= 0 || distance > 2.0) return;
        if (sprinting) {
            breathDistance += distance;
            while (breathDistance >= 100.0) { breathDistance -= 100.0; breath.addExperience(1); }
        }
        if (loadRatio >= 0.70) {
            strengthDistance += distance;
            while (strengthDistance >= 100.0) { strengthDistance -= 100.0; strength.addExperience(1); }
        }
    }

    public void trainHealth(double healed) {
        if (healed <= 0) return;
        healedHealth += healed;
        while (healedHealth >= 20.0) { healedHealth -= 20.0; healthTraining.addExperience(1); }
    }

    public int addExperience(long amount) {
        if (amount <= 0 || level >= LevelCurve.MAX_LEVEL) return 0;
        experience += amount;
        int gained = 0;
        while (level < LevelCurve.MAX_LEVEL && experience >= LevelCurve.requiredExp(level)) {
            experience -= LevelCurve.requiredExp(level++);
            totalSkillPoints++;
            gained++;
        }
        if (level >= LevelCurve.MAX_LEVEL) experience = 0;
        return gained;
    }

    public void setLevel(int value) {
        level = Math.max(LevelCurve.MIN_LEVEL, Math.min(LevelCurve.MAX_LEVEL, value));
        experience = 0;
        totalSkillPoints = Math.max(totalSkillPoints, level - 1);
        spentSkillPoints = Math.min(spentSkillPoints, totalSkillPoints);
    }

    public void setClass(RpgClass value) { rpgClass = value; sheathe(); }
    public void setSpecialization(Specialization value) {
        specialization = value;
        if (value == Specialization.SUCCESSION) activeSet = WeaponSet.MAIN;
        sheathe();
    }
    public void setActiveSet(WeaponSet value) {
        activeSet = specialization == Specialization.SUCCESSION ? WeaponSet.MAIN : value;
        sheathe();
    }

    public boolean equip(ItemStack stack) {
        if (!(stack.getItem() instanceof RpgWeaponItem weapon) || weapon.rpgClass() != rpgClass) return false;
        weapons[weapon.slot().ordinal()] = stack.copyWithCount(1);
        sheathe();
        return true;
    }

    public boolean canDraw() {
        if (!matches(WeaponSlot.MAIN) || !matches(WeaponSlot.SUB)) return false;
        return activeSet != WeaponSet.AWAKENING || matches(WeaponSlot.AWAKENING);
    }

    private boolean matches(WeaponSlot slot) {
        ItemStack stack = weapon(slot);
        return stack.getItem() instanceof RpgWeaponItem weapon
                && weapon.rpgClass() == rpgClass && weapon.slot() == slot;
    }

    public boolean toggleDraw() {
        if (weaponDrawn) { sheathe(); return true; }
        if (!canDraw()) return false;
        weaponDrawn = true;
        combatTicks = COMBAT_TIMEOUT_TICKS;
        return true;
    }
    public boolean drawForSkill() {
        if (!weaponDrawn && !toggleDraw()) return false;
        touchCombat();
        return true;
    }
    public void touchCombat() { if (weaponDrawn) combatTicks = COMBAT_TIMEOUT_TICKS; }
    public boolean tick() {
        protection.tick(inCombat());
        if (combatTicks > 0 && --combatTicks == 0) { weaponDrawn = false; return true; }
        return false;
    }
    public void sheathe() { weaponDrawn = false; combatTicks = 0; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Class", rpgClass.name());
        tag.putString("Specialization", specialization.name());
        tag.putString("ActiveSet", activeSet.name());
        tag.putInt("Level", level);
        tag.putLong("Experience", experience);
        tag.putInt("TotalSkillPoints", totalSkillPoints);
        tag.putInt("SpentSkillPoints", spentSkillPoints);
        tag.put("Breath", breath.save());
        tag.put("Strength", strength.save());
        tag.put("HealthTraining", healthTraining.save());
        tag.putDouble("Stamina", stamina);
        for (WeaponSlot slot : WeaponSlot.values()) tag.put("Weapon_" + slot.name(), weapon(slot).save(new CompoundTag()));
        return tag;
    }
    public void load(CompoundTag tag) {
        try { rpgClass = RpgClass.valueOf(tag.getString("Class")); } catch (Exception ignored) {}
        try { specialization = Specialization.valueOf(tag.getString("Specialization")); } catch (Exception ignored) {}
        try { activeSet = WeaponSet.valueOf(tag.getString("ActiveSet")); } catch (Exception ignored) {}
        level = Math.max(LevelCurve.MIN_LEVEL, Math.min(LevelCurve.MAX_LEVEL, tag.contains("Level") ? tag.getInt("Level") : 1));
        experience = Math.max(0, tag.getLong("Experience"));
        totalSkillPoints = Math.max(0, tag.getInt("TotalSkillPoints"));
        spentSkillPoints = Math.max(0, Math.min(totalSkillPoints, tag.getInt("SpentSkillPoints")));
        if (tag.contains("Breath")) breath.load(tag.getCompound("Breath"));
        if (tag.contains("Strength")) strength.load(tag.getCompound("Strength"));
        if (tag.contains("HealthTraining")) healthTraining.load(tag.getCompound("HealthTraining"));
        stamina = tag.contains("Stamina") ? Math.max(0, Math.min(maxStamina(), tag.getDouble("Stamina"))) : maxStamina();
        for (WeaponSlot slot : WeaponSlot.values()) weapons[slot.ordinal()] = ItemStack.of(tag.getCompound("Weapon_" + slot.name()));
        if (specialization == Specialization.SUCCESSION) activeSet = WeaponSet.MAIN;
        sheathe();
    }
    public void copyFrom(RpgPlayerData other) { load(other.save()); }
}
