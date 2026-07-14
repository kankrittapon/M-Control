package com.zexqm.rpgproject.rpg.skill;

public enum SkillAvailability {
    AVAILABLE,
    METADATA_ONLY,
    WRONG_CLASS,
    WRONG_SPECIALIZATION,
    LEVEL_REQUIRED,
    PREREQUISITE_REQUIRED,
    EXCLUSIVE_CONFLICT,
    MAX_RANK,
    NOT_ENOUGH_SKILL_POINTS,
    UNKNOWN_SKILL
}
