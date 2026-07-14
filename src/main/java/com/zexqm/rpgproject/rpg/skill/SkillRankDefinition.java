package com.zexqm.rpgproject.rpg.skill;

public record SkillRankDefinition(int rank, int requiredLevel, Integer skillPointCost) {
    public SkillRankDefinition {
        if (rank < 1 || requiredLevel < 1 || skillPointCost != null && skillPointCost < 0)
            throw new IllegalArgumentException("Invalid skill rank definition");
    }

    public boolean hasSkillPointCost() { return skillPointCost != null; }
}
