package com.zexqm.rpgproject.client;

import net.minecraft.resources.ResourceLocation;
import com.zexqm.rpgproject.rpg.skill.SkillAvailability;

import java.util.Map;

public final class ClientSkillProgress {
    private static Map<ResourceLocation, Integer> learnedRanks = Map.of();
    private static Map<ResourceLocation, SkillAvailability> availability = Map.of();
    private static int availableSkillPoints;

    public static void apply(Map<ResourceLocation, Integer> ranks,
                             Map<ResourceLocation, SkillAvailability> availabilityBySkill, int points) {
        learnedRanks = Map.copyOf(ranks);
        availability = Map.copyOf(availabilityBySkill);
        availableSkillPoints = Math.max(0, points);
    }

    public static int rank(ResourceLocation skill) { return learnedRanks.getOrDefault(skill, 0); }
    public static Map<ResourceLocation, Integer> learnedRanks() { return learnedRanks; }
    public static SkillAvailability availability(ResourceLocation skill) {
        return availability.getOrDefault(skill, SkillAvailability.UNKNOWN_SKILL);
    }
    public static Map<ResourceLocation, SkillAvailability> availability() { return availability; }
    public static int availableSkillPoints() { return availableSkillPoints; }

    private ClientSkillProgress() {}
}
