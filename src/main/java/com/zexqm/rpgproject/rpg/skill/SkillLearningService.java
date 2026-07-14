package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.rpg.RpgPlayerData;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class SkillLearningService {
    public static SkillAvailability availability(RpgPlayerData data, ResourceLocation skillId) {
        SkillCatalogEntry skill = SkillCatalog.get(skillId);
        if (skill == null) return SkillAvailability.UNKNOWN_SKILL;
        if (!skill.playable()) return SkillAvailability.METADATA_ONLY;
        if (skill.rpgClass() != data.rpgClass()) return SkillAvailability.WRONG_CLASS;
        if (skill.tree() == SkillTree.SUCCESSION
                && data.specialization() != com.zexqm.rpgproject.rpg.Specialization.SUCCESSION
                || skill.tree() == SkillTree.AWAKENING
                && data.specialization() != com.zexqm.rpgproject.rpg.Specialization.AWAKENING)
            return SkillAvailability.WRONG_SPECIALIZATION;
        int current = data.skillProgress().rank(skillId);
        if (current >= skill.maximumRank()) return SkillAvailability.MAX_RANK;
        SkillRankDefinition next = skill.rank(current + 1);
        SkillDefinition combat = SkillRegistry.get(skillId, current + 1);
        if (next == null || combat == null || combat.debugOnly() || combat.rpgClass() != skill.rpgClass())
            return SkillAvailability.METADATA_ONLY;
        if (data.level() < next.requiredLevel()) return SkillAvailability.LEVEL_REQUIRED;
        for (SkillRequirement requirement : skill.prerequisites())
            if (data.skillProgress().rank(requirement.skillId()) < requirement.minimumRank())
                return SkillAvailability.PREREQUISITE_REQUIRED;
        for (ResourceLocation exclusive : skill.mutuallyExclusive())
            if (data.skillProgress().rank(exclusive) > 0) return SkillAvailability.EXCLUSIVE_CONFLICT;
        if (data.availableSkillPoints() < next.skillPointCost()) return SkillAvailability.NOT_ENOUGH_SKILL_POINTS;
        return SkillAvailability.AVAILABLE;
    }

    public static SkillLearningResult upgrade(RpgPlayerData data, ResourceLocation skillId) {
        int previous = data.skillProgress().rank(skillId);
        SkillAvailability availability = availability(data, skillId);
        if (availability != SkillAvailability.AVAILABLE)
            return SkillLearningResult.rejected(availability, previous, data.availableSkillPoints());
        SkillCatalogEntry skill = SkillCatalog.get(skillId);
        SkillRankDefinition next = skill.rank(previous + 1);
        if (!next.hasSkillPointCost())
            return SkillLearningResult.rejected(SkillAvailability.METADATA_ONLY, previous,
                    data.availableSkillPoints());
        data.setLearnedSkillRank(skillId, previous + 1, next.skillPointCost());
        return new SkillLearningResult(true, SkillAvailability.AVAILABLE, previous, previous + 1,
                data.availableSkillPoints());
    }

    public static SkillLearningResult downgrade(RpgPlayerData data, ResourceLocation skillId) {
        int previous = data.skillProgress().rank(skillId);
        if (previous <= 0) return SkillLearningResult.rejected(SkillAvailability.UNKNOWN_SKILL, 0,
                data.availableSkillPoints());
        for (SkillCatalogEntry dependent : SkillCatalog.all()) {
            for (SkillRequirement requirement : dependent.prerequisites()) {
                if (requirement.skillId().equals(skillId) && requirement.minimumRank() >= previous
                        && data.skillProgress().rank(dependent.id()) > 0)
                    return SkillLearningResult.rejected(SkillAvailability.PREREQUISITE_REQUIRED,
                            previous, data.availableSkillPoints());
            }
        }
        SkillCatalogEntry skill = SkillCatalog.get(skillId);
        if (skill == null || skill.rank(previous) == null)
            return SkillLearningResult.rejected(SkillAvailability.UNKNOWN_SKILL, previous,
                    data.availableSkillPoints());
        SkillRankDefinition rank = skill.rank(previous);
        if (!rank.hasSkillPointCost())
            return SkillLearningResult.rejected(SkillAvailability.METADATA_ONLY, previous,
                    data.availableSkillPoints());
        data.setLearnedSkillRank(skillId, previous - 1, -rank.skillPointCost());
        return new SkillLearningResult(true, SkillAvailability.AVAILABLE, previous, previous - 1,
                data.availableSkillPoints());
    }

    public static void reset(RpgPlayerData data) { data.resetLearnedSkills(); }

    public static boolean reconcileSpentSkillPoints(RpgPlayerData data) {
        long recalculated = 0;
        for (Map.Entry<ResourceLocation, Integer> learned : data.skillProgress().learnedRanks().entrySet()) {
            SkillCatalogEntry skill = SkillCatalog.get(learned.getKey());
            if (skill == null || learned.getValue() > skill.maximumRank()) return false;
            for (int rank = 1; rank <= learned.getValue(); rank++) {
                SkillRankDefinition definition = skill.rank(rank);
                if (definition == null || !definition.hasSkillPointCost()) return false;
                recalculated += definition.skillPointCost();
                if (recalculated > Integer.MAX_VALUE) return false;
            }
        }
        data.reconcileSpentSkillPoints((int) recalculated);
        return true;
    }

    private SkillLearningService() {}
}
