package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.rpg.RpgClass;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public record SkillCatalogEntry(ResourceLocation id, String mcpId, String name, String description,
                                RpgClass rpgClass, SkillTree tree, String icon,
                                boolean playable, String unavailableReason,
                                List<SkillRankDefinition> ranks, List<SkillRequirement> prerequisites,
                                Set<ResourceLocation> mutuallyExclusive) {
    public SkillCatalogEntry {
        if (id == null || mcpId == null || mcpId.isBlank() || name == null || name.isBlank()
                || description == null || rpgClass == null || tree == null)
            throw new IllegalArgumentException("Missing skill catalog field");
        ranks = List.copyOf(ranks == null ? List.of() : ranks);
        prerequisites = List.copyOf(prerequisites == null ? List.of() : prerequisites);
        mutuallyExclusive = Set.copyOf(mutuallyExclusive == null ? Set.of() : mutuallyExclusive);
        unavailableReason = unavailableReason == null ? "" : unavailableReason;
        if (playable && ranks.isEmpty()) throw new IllegalArgumentException("Playable skill requires ranks");
        if (playable && ranks.stream().anyMatch(rank -> !rank.hasSkillPointCost()))
            throw new IllegalArgumentException("Playable skill requires SP costs for every rank");
        for (int index = 0; index < ranks.size(); index++) {
            if (ranks.get(index).rank() != index + 1) throw new IllegalArgumentException("Ranks must be contiguous");
        }
    }

    public int maximumRank() { return ranks.size(); }
    public SkillRankDefinition rank(int value) {
        return value < 1 || value > ranks.size() ? null : ranks.get(value - 1);
    }
}
