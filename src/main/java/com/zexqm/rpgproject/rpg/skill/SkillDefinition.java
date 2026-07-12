package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.rpg.*;
import net.minecraft.resources.ResourceLocation;

public record SkillDefinition(ResourceLocation id, RpgClass rpgClass, Specialization specialization,
                              SkillTargetingType targeting, SkillWeaponRequirement weapons,
                              int manaCost, double staminaCost, int cooldownTicks, int castTicks,
                              int recoveryTicks, double range, double radius, double coefficient) {
    public boolean canUse(RpgPlayerData data) {
        return data.rpgClass() == rpgClass
                && (specialization == null || data.specialization() == specialization)
                && data.weaponDrawn() && weapons.test(data) && data.stamina() >= staminaCost;
    }
}
