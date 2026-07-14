package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.resources.ResourceLocation;

public record SkillRequirement(ResourceLocation skillId, int minimumRank) {
    public SkillRequirement {
        if (skillId == null || minimumRank < 1) throw new IllegalArgumentException("Invalid prerequisite");
    }
}
