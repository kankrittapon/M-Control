package com.zexqm.rpgproject.rpg.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Set;

public final class RpgCombatService {
    private static final ThreadLocal<Boolean> APPLYING_TAGGED_DAMAGE = ThreadLocal.withInitial(() -> false);

    public static RpgDamageResult apply(RpgDamageContext context) {
        LivingEntity attacker = context.attacker();
        LivingEntity target = context.target();
        if (!attacker.isAlive() || !target.isAlive() || attacker.level() != target.level()) {
            return RpgDamageResult.stopped(RpgDamageResult.Outcome.INVALID);
        }

        RpgCombatState state = target.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (state == null) return RpgDamageResult.stopped(RpgDamageResult.Outcome.INVALID);
        if (state.iframe()) return RpgDamageResult.stopped(RpgDamageResult.Outcome.IFRAME);
        if (state.pveIframe() && !(attacker instanceof Player))
            return RpgDamageResult.stopped(RpgDamageResult.Outcome.IFRAME);
        if (state.frozen()) return RpgDamageResult.stopped(RpgDamageResult.Outcome.FROZEN_IMMUNE);

        CombatConfig.Values config = CombatConfig.values();
        RpgCombatStats attackerStats = RpgCombatStats.resolve(attacker);
        RpgCombatStats targetStats = RpgCombatStats.resolve(target);
        double hitChance = net.minecraft.util.Mth.clamp(
                RpgCombatMath.hitChance(attackerStats.accuracy(), targetStats.evasion(), config)
                        + context.hitChanceBonus(), config.minimumHitChance(), config.maximumHitChance());
        if (context.accuracyEligible() && target.getRandom().nextDouble() > hitChance) {
            return RpgDamageResult.stopped(RpgDamageResult.Outcome.MISS);
        }

        double power = switch (context.powerType()) {
            case ATTACK -> attackerStats.attackPower();
            case MAGIC -> attackerStats.magicPower();
            case NONE -> 0.0;
        };
        double damage = (context.baseDamage() + power * config.powerScale()) * context.coefficient();
        double criticalChance = net.minecraft.util.Mth.clamp(
                attackerStats.criticalChance() + context.criticalChanceBonus(), 0.0, 1.0);
        boolean critical = context.criticalEligible()
                && attacker.getRandom().nextDouble() < criticalChance;
        if (critical) damage *= attackerStats.criticalDamage();

        Set<SpecialAttackType> specials = resolveSpecials(context, state);
        damage = RpgCombatMath.stackSpecialMultipliers(damage, specials, config);

        CombatImpactContext impact = CombatImpactContext.resolve(target, RpgDamageTypes.source(attacker),
                (float) damage, CombatImpactCategory.RPG, context.origin(), true,
                attacker, attacker, false);
        ProtectionDecision protection = ProtectionResolver.resolveDamage(state, impact);
        if (protection.blockDamage() && (!protection.consumeGuard() || state.absorbGuard(damage))) {
            return new RpgDamageResult(RpgDamageResult.Outcome.GUARDED, 0.0, critical, specials,
                    new CrowdControlApplicationResult(CrowdControlApplicationResult.Status.FRONT_GUARD,
                            context.crowdControl(), 0.0, 0));
        }

        damage = RpgCombatMath.reducedDamage(damage, targetStats.damageReduction(), config);
        damage = TimedDamageReductionService.reduce(target, (float) Math.max(0.0, damage));
        float applied = ManaShieldService.absorb(target, (float) Math.max(0.0, damage)).remainingDamage();
        APPLYING_TAGGED_DAMAGE.set(true);
        boolean hurt;
        try {
            hurt = target.hurt(RpgDamageTypes.source(attacker), applied);
        } finally {
            APPLYING_TAGGED_DAMAGE.remove();
        }
        if (!hurt) return RpgDamageResult.stopped(RpgDamageResult.Outcome.INVALID);

        CrowdControlApplicationResult cc = CrowdControlResolver.apply(target, context.crowdControl(), context.origin());
        return new RpgDamageResult(RpgDamageResult.Outcome.HIT, applied, critical, specials, cc);
    }

    public static boolean applyingTaggedDamage() {
        return APPLYING_TAGGED_DAMAGE.get();
    }

    private static Set<SpecialAttackType> resolveSpecials(RpgDamageContext context, RpgCombatState targetState) {
        EnumSet<SpecialAttackType> result = EnumSet.noneOf(SpecialAttackType.class);
        Set<SpecialAttackType> eligible = context.specialAttackEligibility();
        CombatConfig.Values config = CombatConfig.values();
        if (eligible.contains(SpecialAttackType.BACK_ATTACK)
                && RpgCombatMath.withinRearArc(context.target(), context.origin(), config.rearAttackArcDegrees()))
            result.add(SpecialAttackType.BACK_ATTACK);
        if (eligible.contains(SpecialAttackType.DOWN_ATTACK) && targetState.downed())
            result.add(SpecialAttackType.DOWN_ATTACK);
        if (eligible.contains(SpecialAttackType.AIR_ATTACK) && targetState.floated())
            result.add(SpecialAttackType.AIR_ATTACK);
        if (eligible.contains(SpecialAttackType.SPEED_ATTACK) && movingForward(context.target()))
            result.add(SpecialAttackType.SPEED_ATTACK);
        if (eligible.contains(SpecialAttackType.COUNTER_ATTACK) && targetState.casting())
            result.add(SpecialAttackType.COUNTER_ATTACK);
        return result;
    }

    private static boolean movingForward(LivingEntity target) {
        var movement = target.getDeltaMovement().multiply(1, 0, 1);
        var forward = target.getLookAngle().multiply(1, 0, 1);
        return movement.lengthSqr() > 1.0E-4 && movement.normalize().dot(forward.normalize()) > 0.5;
    }

    private RpgCombatService() {}
}
