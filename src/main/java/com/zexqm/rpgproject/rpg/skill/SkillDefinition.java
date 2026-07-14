package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.RpgClass;
import com.zexqm.rpgproject.rpg.Specialization;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import com.zexqm.rpgproject.rpg.status.StatusStackingPolicy;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public record SkillDefinition(ResourceLocation id, boolean debugOnly, RpgClass rpgClass,
                              Specialization specialization, int rank,
                              SkillTargetingType targeting, SkillWeaponRequirement weapons,
                              PrimaryResourceType resourceType, int resourceCost, double staminaCost,
                              int cooldownTicks, int castTicks, int recoveryTicks,
                              MovementPolicy movementPolicy, CancelPolicy cancelPolicy,
                              double range, double radius, List<Hit> hits,
                              List<ProtectionWindow> protectionWindows) {
    public SkillDefinition {
        if (id == null || targeting == null || weapons == null || resourceType == null
                || movementPolicy == null || cancelPolicy == null) throw new IllegalArgumentException("Missing skill field");
        if (rank < 0 || resourceCost < 0 || staminaCost < 0 || cooldownTicks < 0 || castTicks < 0
                || recoveryTicks < 0 || range < 0 || radius < 0) throw new IllegalArgumentException("Negative skill value");
        hits = List.copyOf(hits == null ? List.of() : hits);
        protectionWindows = List.copyOf(protectionWindows == null ? List.of() : protectionWindows);
    }

    public record Hit(int timingTick, double baseDamage, double coefficient, double radius,
                      RpgPowerType powerType, CrowdControlType crowdControl,
                      Set<SpecialAttackType> specialAttacks, List<StatusPayload> statuses) {
        public Hit {
            if (timingTick < 0 || baseDamage < 0 || coefficient < 0 || radius < 0)
                throw new IllegalArgumentException("Invalid hit values");
            powerType = powerType == null ? RpgPowerType.NONE : powerType;
            specialAttacks = Set.copyOf(specialAttacks == null ? Set.of() : specialAttacks);
            statuses = List.copyOf(statuses == null ? List.of() : statuses);
        }
    }

    public record StatusPayload(RpgStatusType type, int durationTicks, int intervalTicks,
                                double potency, int maxStacks, StatusStackingPolicy stacking,
                                Set<MobControlProfile> allowedProfiles) {
        public StatusPayload {
            allowedProfiles = Set.copyOf(allowedProfiles == null ? Set.of() : allowedProfiles);
        }
    }

    public record ProtectionWindow(ProtectionType type, int fromTick, int toTick) {
        public boolean active(int tick) { return tick >= fromTick && tick <= toTick; }
    }
}
