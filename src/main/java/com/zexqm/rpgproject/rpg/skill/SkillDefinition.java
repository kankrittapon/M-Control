package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.RpgClass;
import com.zexqm.rpgproject.rpg.Specialization;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import com.zexqm.rpgproject.rpg.combat.SpecialAttackType;
import com.zexqm.rpgproject.rpg.combat.SmashType;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import com.zexqm.rpgproject.rpg.status.StatusStackingPolicy;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public record SkillDefinition(ResourceLocation id, boolean debugOnly, boolean innate, RpgClass rpgClass,
                              Specialization specialization, int rank,
                              SkillTargetingType targeting, SkillWeaponRequirement weapons,
                              PrimaryResourceType resourceType, int resourceCost, double staminaCost,
                              int cooldownTicks, int castTicks, int recoveryTicks,
                              MovementPolicy movementPolicy, CancelPolicy cancelPolicy,
                              double range, double radius, List<Hit> hits,
                              List<ProtectionWindow> protectionWindows,
                              CooldownRecastPolicy cooldownRecast, SkillLinkPolicy links,
                              TransitionPolicy transitions, double projectileSpeed,
                              FacingPolicy facingPolicy, double turnSpeed,
                              double castMpRecoveryPercent, CasterMovementType casterMovementType,
                              double casterLateralDistance, SkillAimMode aimMode,
                              int targetingTimeoutTicks) {
    public SkillDefinition {
        if (id == null || targeting == null || weapons == null || resourceType == null
                || movementPolicy == null || cancelPolicy == null || casterMovementType == null
                || aimMode == null)
            throw new IllegalArgumentException("Missing skill field");
        if (rank < 0 || resourceCost < 0 || staminaCost < 0 || cooldownTicks < 0 || castTicks < 0
                || recoveryTicks < 0 || range < 0 || radius < 0 || projectileSpeed < 0 || turnSpeed < 0
                || !Double.isFinite(castMpRecoveryPercent) || castMpRecoveryPercent < 0
                || castMpRecoveryPercent > 1 || !Double.isFinite(casterLateralDistance)
                || casterLateralDistance < 0 || targetingTimeoutTicks < 0)
            throw new IllegalArgumentException("Negative skill value");
        if (targeting == SkillTargetingType.AIM_PROJECTILE && projectileSpeed <= 0)
            throw new IllegalArgumentException("Aim projectile skills require projectile_speed");
        if (aimMode == SkillAimMode.CONFIRM_TARGETING && targetingTimeoutTicks < 1)
            throw new IllegalArgumentException("Confirm-targeted skills require targeting_timeout_ticks");
        hits = List.copyOf(hits == null ? List.of() : hits);
        protectionWindows = List.copyOf(protectionWindows == null ? List.of() : protectionWindows);
        cooldownRecast = cooldownRecast == null ? CooldownRecastPolicy.DISABLED : cooldownRecast;
        links = links == null ? SkillLinkPolicy.NONE : links;
        transitions = transitions == null ? TransitionPolicy.NONE : transitions;
        facingPolicy = facingPolicy == null ? FacingPolicy.NONE : facingPolicy;
    }

    public SkillDefinition(ResourceLocation id, boolean debugOnly, RpgClass rpgClass,
                           Specialization specialization, int rank, SkillTargetingType targeting,
                           SkillWeaponRequirement weapons, PrimaryResourceType resourceType,
                           int resourceCost, double staminaCost, int cooldownTicks, int castTicks,
                           int recoveryTicks, MovementPolicy movementPolicy, CancelPolicy cancelPolicy,
                           double range, double radius, List<Hit> hits,
                           List<ProtectionWindow> protectionWindows) {
        this(id, debugOnly, false, rpgClass, specialization, rank, targeting, weapons, resourceType,
                resourceCost, staminaCost, cooldownTicks, castTicks, recoveryTicks, movementPolicy,
                cancelPolicy, range, radius, hits, protectionWindows, CooldownRecastPolicy.DISABLED,
                SkillLinkPolicy.NONE, TransitionPolicy.fromLegacy(cancelPolicy),
                targeting == SkillTargetingType.AIM_PROJECTILE ? 1.0 : 0.0,
                FacingPolicy.NONE, 0.0, 0.0, CasterMovementType.NONE, 0.0,
                SkillAimMode.INSTANT_AIM, 0);
    }

    public record TransitionPolicy(int movementCancelFromTick, boolean movementUntilFirstHit,
                                   int skillCancelFromTick, boolean skillUntilFirstHit,
                                   boolean interruptsCasting) {
        public static final TransitionPolicy NONE = new TransitionPolicy(-1, false, -1, false, false);

        public TransitionPolicy {
            if (movementCancelFromTick < -1 || skillCancelFromTick < -1)
                throw new IllegalArgumentException("Invalid transition tick");
        }

        public boolean allows(SkillCancelTrigger trigger, int tick, int firstHitTick) {
            int from = trigger == SkillCancelTrigger.MOVEMENT ? movementCancelFromTick : skillCancelFromTick;
            boolean untilHit = trigger == SkillCancelTrigger.MOVEMENT ? movementUntilFirstHit : skillUntilFirstHit;
            return from >= 0 && tick >= from && (!untilHit || tick < firstHitTick);
        }

        public static TransitionPolicy fromLegacy(CancelPolicy policy) {
            return switch (policy) {
                case NEVER -> NONE;
                case BEFORE_FIRST_HIT -> new TransitionPolicy(0, true, 0, true, false);
                case ANY_TIME -> new TransitionPolicy(0, false, 0, false, false);
            };
        }
    }

    public record CooldownRecastPolicy(boolean enabled, double damageMultiplier,
                                       boolean allowCritical, boolean allowSpecialAttacks,
                                       boolean allowCrowdControl, boolean allowSmash,
                                       boolean allowStatuses, boolean allowResources,
                                       boolean allowProtection) {
        public static final CooldownRecastPolicy DISABLED = new CooldownRecastPolicy(
                false, 0, false, false, false, false, false, false, false);

        public CooldownRecastPolicy {
            if (!Double.isFinite(damageMultiplier) || damageMultiplier < 0 || damageMultiplier > 1)
                throw new IllegalArgumentException("Cooldown recast damage multiplier must be between 0 and 1");
            if (enabled && damageMultiplier <= 0)
                throw new IllegalArgumentException("Enabled cooldown recast requires positive damage");
        }
    }

    public record SkillLinkPolicy(ResourceLocation grants, int grantDurationTicks,
                                  SkillLinkTiming grantTiming, ResourceLocation requires,
                                  boolean consumeRequiredOnCastStart, boolean useRequiredAnchor) {
        public static final SkillLinkPolicy NONE = new SkillLinkPolicy(null, 0,
                SkillLinkTiming.CAST_START, null, false, false);

        public SkillLinkPolicy {
            grantTiming = grantTiming == null ? SkillLinkTiming.CAST_START : grantTiming;
            if (grantDurationTicks < 0 || (grants != null && grantDurationTicks < 1))
                throw new IllegalArgumentException("Granted skill links require a positive duration");
            if (consumeRequiredOnCastStart && requires == null)
                throw new IllegalArgumentException("Cannot consume a missing required link ID");
            if (useRequiredAnchor && requires == null)
                throw new IllegalArgumentException("Cannot use an anchor without a required link ID");
        }
    }

    public record Hit(int timingTick, double baseDamage, double coefficient, double radius,
                      RpgPowerType powerType, CrowdControlType crowdControl,
                      CrowdControlType playerCrowdControl, double pullStrength,
                      Set<SpecialAttackType> specialAttacks, List<StatusPayload> statuses,
                      List<SmashPayload> smashes, ResourcePayload resources,
                      SkillImpactShape impactShape, int maxTargets,
                      double forwardOffset, double rightOffset,
                      double hitChanceBonus, double criticalChanceBonus,
                      double additionalTargetDamagePenalty, double minimumTargetDamageMultiplier,
                      SkillTargetDisposition targetDisposition, HealthPayload health,
                      DefensivePayload defensive, TauntBeaconPayload tauntBeacon) {
        public Hit {
            if (timingTick < 0 || baseDamage < 0 || coefficient < 0 || radius < 0
                    || maxTargets < 0 || !Double.isFinite(forwardOffset) || !Double.isFinite(rightOffset)
                    || !Double.isFinite(pullStrength) || pullStrength < 0 || pullStrength > 1
                    || hitChanceBonus < 0 || hitChanceBonus > 1
                    || criticalChanceBonus < 0 || criticalChanceBonus > 1
                    || additionalTargetDamagePenalty < 0 || additionalTargetDamagePenalty > 1
                    || minimumTargetDamageMultiplier < 0 || minimumTargetDamageMultiplier > 1)
                throw new IllegalArgumentException("Invalid hit values");
            powerType = powerType == null ? RpgPowerType.NONE : powerType;
            targetDisposition = targetDisposition == null ? SkillTargetDisposition.HOSTILE : targetDisposition;
            health = health == null ? HealthPayload.NONE : health;
            defensive = defensive == null ? DefensivePayload.NONE : defensive;
            tauntBeacon = tauntBeacon == null ? TauntBeaconPayload.NONE : tauntBeacon;
            specialAttacks = Set.copyOf(specialAttacks == null ? Set.of() : specialAttacks);
            statuses = List.copyOf(statuses == null ? List.of() : statuses);
            smashes = List.copyOf(smashes == null ? List.of() : smashes);
            resources = resources == null ? ResourcePayload.NONE : resources;
            impactShape = impactShape == null ? SkillImpactShape.AUTO : impactShape;
        }

        public Hit(int timingTick, double baseDamage, double coefficient, double radius,
                   RpgPowerType powerType, CrowdControlType crowdControl,
                   Set<SpecialAttackType> specialAttacks, List<StatusPayload> statuses,
                   List<SmashPayload> smashes) {
            this(timingTick, baseDamage, coefficient, radius, powerType, crowdControl,
                    null, 0, specialAttacks, statuses, smashes, ResourcePayload.NONE,
                    SkillImpactShape.AUTO, 0, 0, 0, 0, 0, 0, 1,
                    SkillTargetDisposition.HOSTILE, HealthPayload.NONE, DefensivePayload.NONE,
                    TauntBeaconPayload.NONE);
        }

        public double targetDamageMultiplier(int targetIndex) {
            return Math.max(minimumTargetDamageMultiplier,
                    1.0 - additionalTargetDamagePenalty * Math.max(0, targetIndex));
        }
    }

    public record TauntBeaconPayload(int durationTicks, int refreshIntervalTicks, double radius,
                                     int maxTargets, double placementDistance,
                                     Set<MobControlProfile> allowedProfiles) {
        public static final TauntBeaconPayload NONE = new TauntBeaconPayload(0, 0, 0, 0, 0, Set.of());

        public TauntBeaconPayload {
            allowedProfiles = Set.copyOf(allowedProfiles == null ? Set.of() : allowedProfiles);
            if (durationTicks < 0 || refreshIntervalTicks < 0 || radius < 0 || maxTargets < 0
                    || placementDistance < 0 || !Double.isFinite(radius)
                    || !Double.isFinite(placementDistance))
                throw new IllegalArgumentException("Invalid taunt beacon payload");
            if (durationTicks > 0 && (refreshIntervalTicks < 1 || radius <= 0 || maxTargets < 1))
                throw new IllegalArgumentException("Active taunt beacon requires interval, radius, and targets");
        }

        public boolean active() { return durationTicks > 0; }
    }

    public record DefensivePayload(int manaShieldTicks, double manaShieldRatio,
                                   int resistanceTicks, double resistanceBonus,
                                   int damageReductionTicks, double damageReductionRatio,
                                   int sustainedResourceTicks, int resourceIntervalTicks,
                                   int flatMpRecovery, double movementSpeedBonus,
                                   int speedBuffTicks, double attackSpeedBonus,
                                   double castingSpeedBonus, double timedMovementSpeedBonus,
                                   int castTimeOverrideTicks) {
        public static final DefensivePayload NONE = new DefensivePayload(0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0);

        public DefensivePayload(int manaShieldTicks, double manaShieldRatio,
                                int resistanceTicks, double resistanceBonus,
                                int damageReductionTicks, double damageReductionRatio) {
            this(manaShieldTicks, manaShieldRatio, resistanceTicks, resistanceBonus,
                    damageReductionTicks, damageReductionRatio, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public DefensivePayload {
            if (manaShieldTicks < 0 || resistanceTicks < 0 || damageReductionTicks < 0
                    || sustainedResourceTicks < 0 || resourceIntervalTicks < 0 || flatMpRecovery < 0
                    || speedBuffTicks < 0 || castTimeOverrideTicks < 0
                    || !Double.isFinite(manaShieldRatio) || manaShieldRatio < 0 || manaShieldRatio > 1
                    || !Double.isFinite(resistanceBonus) || resistanceBonus < 0 || resistanceBonus > 1
                    || !Double.isFinite(damageReductionRatio)
                    || damageReductionRatio < 0 || damageReductionRatio > 1
                    || !Double.isFinite(movementSpeedBonus) || movementSpeedBonus < 0
                    || !Double.isFinite(attackSpeedBonus) || attackSpeedBonus < 0
                    || !Double.isFinite(castingSpeedBonus) || castingSpeedBonus < 0
                    || !Double.isFinite(timedMovementSpeedBonus) || timedMovementSpeedBonus < 0)
                throw new IllegalArgumentException("Invalid defensive payload");
            if ((manaShieldRatio > 0) != (manaShieldTicks > 0)
                    || (resistanceBonus > 0) != (resistanceTicks > 0)
                    || (damageReductionRatio > 0) != (damageReductionTicks > 0)
                    || (sustainedResourceTicks > 0) != (resourceIntervalTicks > 0 && flatMpRecovery > 0)
                    || (speedBuffTicks > 0) != (attackSpeedBonus > 0 || castingSpeedBonus > 0
                    || timedMovementSpeedBonus > 0))
                throw new IllegalArgumentException("Defensive effect requires value and duration");
        }

        public boolean active() {
            return manaShieldTicks > 0 || resistanceTicks > 0 || damageReductionTicks > 0
                    || sustainedResourceTicks > 0 || speedBuffTicks > 0 || castTimeOverrideTicks > 0;
        }
    }

    public record HealthPayload(double maxHealthRecoveryPercent, int flatHealthRecovery,
                                double allyMaxHealthRecoveryPercent, int allyFlatHealthRecovery) {
        public static final HealthPayload NONE = new HealthPayload(0, 0, 0, 0);

        public HealthPayload(double maxHealthRecoveryPercent, int flatHealthRecovery) {
            this(maxHealthRecoveryPercent, flatHealthRecovery,
                    maxHealthRecoveryPercent, flatHealthRecovery);
        }

        public HealthPayload {
            if (!Double.isFinite(maxHealthRecoveryPercent) || maxHealthRecoveryPercent < 0
                    || maxHealthRecoveryPercent > 1 || flatHealthRecovery < 0
                    || !Double.isFinite(allyMaxHealthRecoveryPercent)
                    || allyMaxHealthRecoveryPercent < 0 || allyMaxHealthRecoveryPercent > 1
                    || allyFlatHealthRecovery < 0)
                throw new IllegalArgumentException("Invalid health recovery payload");
        }

        public boolean active() {
            return maxHealthRecoveryPercent > 0 || flatHealthRecovery > 0
                    || allyMaxHealthRecoveryPercent > 0 || allyFlatHealthRecovery > 0;
        }

        public double recoveryPercent(boolean caster) {
            return caster ? maxHealthRecoveryPercent : allyMaxHealthRecoveryPercent;
        }

        public int flatRecovery(boolean caster) {
            return caster ? flatHealthRecovery : allyFlatHealthRecovery;
        }
    }

    public record ResourcePayload(double maxMpRecoveryPercent, int flatMpRecovery,
                                  double targetMaxResourceDrainPercent,
                                  double drainTransferRatio, boolean recoverOncePerHitWindow) {
        public static final ResourcePayload NONE = new ResourcePayload(0, 0, 0, 0, true);

        public ResourcePayload {
            if (!percent(maxMpRecoveryPercent) || flatMpRecovery < 0 || !percent(targetMaxResourceDrainPercent)
                    || !percent(drainTransferRatio))
                throw new IllegalArgumentException("Resource payload percentages must be between 0 and 1");
        }

        public ResourcePayload(double maxMpRecoveryPercent, double targetMaxResourceDrainPercent,
                               double drainTransferRatio, boolean recoverOncePerHitWindow) {
            this(maxMpRecoveryPercent, 0, targetMaxResourceDrainPercent, drainTransferRatio,
                    recoverOncePerHitWindow);
        }

        private static boolean percent(double value) {
            return Double.isFinite(value) && value >= 0 && value <= 1;
        }
    }

    public record SmashPayload(SmashType type, double chance) {
        public SmashPayload {
            if (type == null || chance < 0.0 || chance > 1.0)
                throw new IllegalArgumentException("Invalid smash payload");
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
