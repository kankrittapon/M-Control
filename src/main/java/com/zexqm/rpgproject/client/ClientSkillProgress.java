package com.zexqm.rpgproject.client;

import net.minecraft.resources.ResourceLocation;
import com.zexqm.rpgproject.rpg.skill.SkillAvailability;

import java.util.Map;

public final class ClientSkillProgress {
    private static Map<ResourceLocation, Integer> learnedRanks = Map.of();
    private static Map<ResourceLocation, SkillAvailability> availability = Map.of();
    private static int totalSkillPoints;
    private static int spentSkillPoints;
    private static long skillExperience;
    private static long requiredSkillExperience;

    public static void apply(Map<ResourceLocation, Integer> ranks,
                             Map<ResourceLocation, SkillAvailability> availabilityBySkill,
                             int totalPoints, int spentPoints, long experience, long requiredExperience) {
        learnedRanks = Map.copyOf(ranks);
        availability = Map.copyOf(availabilityBySkill);
        totalSkillPoints = Math.max(0, totalPoints);
        spentSkillPoints = Math.max(0, Math.min(totalSkillPoints, spentPoints));
        skillExperience = Math.max(0, experience);
        requiredSkillExperience = Math.max(1, requiredExperience);
    }

    public static int rank(ResourceLocation skill) { return learnedRanks.getOrDefault(skill, 0); }
    public static Map<ResourceLocation, Integer> learnedRanks() { return learnedRanks; }
    public static SkillAvailability availability(ResourceLocation skill) {
        return availability.getOrDefault(skill, SkillAvailability.UNKNOWN_SKILL);
    }
    public static Map<ResourceLocation, SkillAvailability> availability() { return availability; }
    public static int totalSkillPoints() { return totalSkillPoints; }
    public static int spentSkillPoints() { return spentSkillPoints; }
    public static int availableSkillPoints() { return totalSkillPoints - spentSkillPoints; }
    public static long skillExperience() { return skillExperience; }
    public static long requiredSkillExperience() { return requiredSkillExperience; }

    private ClientSkillProgress() {}
}
