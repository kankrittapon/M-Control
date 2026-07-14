package com.zexqm.rpgproject.rpg.skill;

public record SkillLearningResult(boolean success, SkillAvailability availability,
                                  int previousRank, int currentRank, int availableSkillPoints) {
    public static SkillLearningResult rejected(SkillAvailability availability, int rank, int points) {
        return new SkillLearningResult(false, availability, rank, rank, points);
    }
}
