package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.ManaProvider;
import com.zexqm.rpgproject.rpg.RpgPlayerData;
import com.zexqm.rpgproject.rpg.RpgPlayerDataProvider;
import com.zexqm.rpgproject.rpg.combat.RpgCombatService;
import com.zexqm.rpgproject.rpg.combat.RpgCombatState;
import com.zexqm.rpgproject.rpg.combat.RpgCombatStateProvider;
import com.zexqm.rpgproject.rpg.combat.RpgDamageContext;
import com.zexqm.rpgproject.rpg.status.RpgStatusService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillRuntime {
    private static final Map<UUID, ActiveCast> ACTIVE = new HashMap<>();
    private static final Map<UUID, PendingCast> PENDING = new HashMap<>();

    public static SkillCastResult cast(ServerPlayer player, ResourceLocation skillId, SkillExecutionContext context) {
        RpgProject.LOGGER.info("[RPG Skill] request player={} skill={} direction={} target={} ground={}",
                player.getScoreboardName(), skillId, context.direction(), context.targetEntityId(), context.groundPosition());
        SkillDefinition skill = SkillRegistry.get(skillId);
        if (skill == null) return result(player, skillId, SkillCastResult.UNKNOWN_SKILL, "not registered");
        if (skill.debugOnly() && !player.hasPermissions(2))
            return result(player, skillId, SkillCastResult.DEBUG_FORBIDDEN, "permission<2");
        RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
        RpgCombatState combat = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (data == null || combat == null || combat.actionLocked() || ACTIVE.containsKey(player.getUUID())
                || PENDING.containsKey(player.getUUID()))
            return result(player, skillId, SkillCastResult.INVALID_STATE, "capability/lock/active");
        if (data.rpgClass() != skill.rpgClass())
            return result(player, skillId, SkillCastResult.WRONG_CLASS, "player=" + data.rpgClass());
        if (skill.specialization() != null && data.specialization() != skill.specialization())
            return result(player, skillId, SkillCastResult.WRONG_SPECIALIZATION, "player=" + data.specialization());
        if (!skill.weapons().test(data))
            return result(player, skillId, SkillCastResult.WRONG_WEAPON, "requirements=" + skill.weapons());
        if (PrimaryResourceType.forClass(data.rpgClass()) != skill.resourceType())
            return result(player, skillId, SkillCastResult.WRONG_RESOURCE, "required=" + skill.resourceType());
        if (!skill.debugOnly() && data.skillProgress().rank(skillId) <= 0)
            return result(player, skillId, SkillCastResult.NOT_LEARNED, "rank=0");
        if (data.actionState() == SkillActionState.SHEATHED) {
            if (!validTarget(skill, context))
                return result(player, skillId, SkillCastResult.INVALID_TARGET, "auto-draw target validation");
            if (!data.requestToggleDraw(SkillRuntimeConfig.values().drawTicks(),
                    SkillRuntimeConfig.values().sheatheTicks()))
                return result(player, skillId, SkillCastResult.INVALID_STATE, "draw rejected");
            PENDING.put(player.getUUID(), new PendingCast(skillId, context));
            return result(player, skillId, SkillCastResult.STARTED, "queued auto-draw");
        }
        if (data.actionState() != SkillActionState.READY)
            return result(player, skillId, SkillCastResult.INVALID_STATE, "action=" + data.actionState());
        long gameTime = player.serverLevel().getServer().overworld().getGameTime();
        if (!data.cooldownReady(skill.id(), gameTime))
            return result(player, skillId, SkillCastResult.COOLDOWN,
                    "remaining=" + data.cooldownRemaining(skill.id(), gameTime));
        var mana = player.getCapability(ManaProvider.MANA).orElse(null);
        if (mana == null || mana.getMana() < skill.resourceCost() || data.stamina() < skill.staminaCost())
            return result(player, skillId, SkillCastResult.INSUFFICIENT_RESOURCE,
                    "primary=" + (mana == null ? "missing" : mana.getMana()) + " stamina=" + data.stamina());
        if (!validTarget(skill, context))
            return result(player, skillId, SkillCastResult.INVALID_TARGET, "range/direction/entity");

        mana.spend(skill.resourceCost());
        data.spendStamina(skill.staminaCost());
        data.startCooldown(skill.id(), gameTime + skill.cooldownTicks());
        data.startCast(skill.castTicks());
        combat.setCasting(true);
        ACTIVE.put(player.getUUID(), new ActiveCast(skill, context, 0, false));
        return result(player, skillId, SkillCastResult.STARTED, "castTicks=" + skill.castTicks()
                + " recoveryTicks=" + skill.recoveryTicks() + " mpCost=" + skill.resourceCost()
                + " staminaCost=" + skill.staminaCost());
    }

    public static boolean tick(ServerPlayer player) {
        PendingCast pending = PENDING.get(player.getUUID());
        if (pending != null) {
            RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
            if (data == null || data.actionState() == SkillActionState.SHEATHED) {
                PENDING.remove(player.getUUID());
                return true;
            }
            if (data.actionState() == SkillActionState.READY) {
                PENDING.remove(player.getUUID());
                SkillExecutionContext refreshed = new SkillExecutionContext(player, player.getEyePosition(),
                        pending.context.direction(), pending.context.targetEntityId(), pending.context.groundPosition());
                cast(player, pending.skillId, refreshed);
                return true;
            }
            return true;
        }
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active == null) return false;
        RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
        RpgCombatState combat = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (data == null || combat == null || combat.actionLocked()) {
            cancel(player);
            return true;
        }
        if (!active.recovering) {
            applyProtection(active.skill, active.tick, combat, player);
            for (SkillDefinition.Hit hit : active.skill.hits()) {
                if (hit.timingTick() == active.tick) executeHit(active.skill, hit, active.context);
            }
            active.tick++;
            if (active.tick > active.skill.castTicks()) {
                combat.setCasting(false);
                data.startRecovery(active.skill.recoveryTicks());
                active.recovering = true;
                active.tick = 0;
                RpgProject.LOGGER.info("[RPG Skill] recovery player={} skill={} ticks={}",
                        player.getScoreboardName(), active.skill.id(), active.skill.recoveryTicks());
            }
        } else if (++active.tick >= active.skill.recoveryTicks()) {
            data.finishRecovery();
            ACTIVE.remove(player.getUUID());
            RpgProject.LOGGER.info("[RPG Skill] complete player={} skill={}",
                    player.getScoreboardName(), active.skill.id());
        }
        return true;
    }

    public static void cancel(ServerPlayer player) {
        ActiveCast active = ACTIVE.remove(player.getUUID());
        PendingCast pending = PENDING.remove(player.getUUID());
        if (active != null || pending != null) RpgProject.LOGGER.info("[RPG Skill] cancel player={} skill={}",
                player.getScoreboardName(), active != null ? active.skill.id() : pending.skillId());
        player.getCapability(RpgCombatStateProvider.DATA).ifPresent(state -> state.setCasting(false));
        player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(RpgPlayerData::cancelAction);
    }

    public static MovementPolicy movementPolicy(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        return active == null ? MovementPolicy.FULL : active.skill.movementPolicy();
    }

    private static void executeHit(SkillDefinition skill, SkillDefinition.Hit hit, SkillExecutionContext context) {
        var targets = SkillHitResolver.resolve(skill, hit, context);
        RpgProject.LOGGER.info("[RPG Skill] hit-window player={} skill={} tick={} shape={} targets={}",
                context.caster().getScoreboardName(), skill.id(), hit.timingTick(), skill.targeting(), targets.size());
        SkillDebugVisualizer.show(skill, hit, context, targets);
        for (var target : targets) {
            var result = RpgCombatService.apply(new RpgDamageContext(context.caster(), target, context.origin(),
                    hit.baseDamage(), hit.powerType(), hit.coefficient(), true, true,
                    hit.crowdControl(), hit.specialAttacks()));
            RpgProject.LOGGER.info("[RPG Skill] damage skill={} target={}#{} outcome={} damage={} specials={} cc={}",
                    skill.id(), target.getType().toShortString(), target.getId(), result.outcome(),
                    result.damage(), result.specialAttacks(), result.crowdControl());
            if (result.outcome() != com.zexqm.rpgproject.rpg.combat.RpgDamageResult.Outcome.HIT) continue;
            for (SkillDefinition.StatusPayload status : hit.statuses()) {
                var statusResult = RpgStatusService.apply(context.caster(), target, status.type(), status.durationTicks(),
                        status.intervalTicks(), status.potency(), status.maxStacks(), status.stacking(),
                        status.allowedProfiles());
                RpgProject.LOGGER.info("[RPG Skill] status skill={} target={}#{} type={} result={} duration={} potency={}",
                        skill.id(), target.getType().toShortString(), target.getId(), status.type(), statusResult,
                        status.durationTicks(), status.potency());
            }
        }
    }

    private static SkillCastResult result(ServerPlayer player, ResourceLocation skill,
                                          SkillCastResult result, String detail) {
        RpgProject.LOGGER.info("[RPG Skill] result player={} skill={} result={} detail={}",
                player.getScoreboardName(), skill, result, detail);
        return result;
    }

    private static void applyProtection(SkillDefinition skill, int tick, RpgCombatState state,
                                        ServerPlayer player) {
        for (SkillDefinition.ProtectionWindow window : skill.protectionWindows()) {
            if (!window.active(tick)) continue;
            if (window.fromTick() == tick) RpgProject.LOGGER.info(
                    "[RPG Skill] protection player={} skill={} type={} fromTick={} toTick={}",
                    player.getScoreboardName(), skill.id(), window.type(), window.fromTick(), window.toTick());
            switch (window.type()) {
                case FRONT_GUARD -> state.activateFrontGuard(2);
                case SUPER_ARMOR -> state.activateSuperArmor(2);
                case IFRAME -> state.activateIframe(2);
                case GRAB_IMMUNITY -> state.activateGrabImmunity(2);
            }
        }
    }

    private static boolean validTarget(SkillDefinition skill, SkillExecutionContext context) {
        if (context.direction().lengthSqr() < 0.99 || context.direction().lengthSqr() > 1.01) return false;
        if (skill.targeting() == SkillTargetingType.ENTITY_TARGETED) {
            if (context.targetEntityId() == null) return false;
            var entity = playerLevel(context).getEntity(context.targetEntityId());
            return entity instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()
                    && living != context.caster() && living.distanceToSqr(context.caster()) <= skill.range() * skill.range();
        }
        if (skill.targeting() == SkillTargetingType.GROUND_AOE
                || skill.targeting() == SkillTargetingType.CIRCLE) {
            return context.groundPosition() != null
                    && context.groundPosition().distanceToSqr(context.origin()) <= skill.range() * skill.range();
        }
        return true;
    }

    private static net.minecraft.server.level.ServerLevel playerLevel(SkillExecutionContext context) {
        return context.caster().serverLevel();
    }

    private static final class ActiveCast {
        private final SkillDefinition skill;
        private final SkillExecutionContext context;
        private int tick;
        private boolean recovering;

        private ActiveCast(SkillDefinition skill, SkillExecutionContext context, int tick, boolean recovering) {
            this.skill = skill;
            this.context = context;
            this.tick = tick;
            this.recovering = recovering;
        }
    }

    private record PendingCast(ResourceLocation skillId, SkillExecutionContext context) {}

    private SkillRuntime() {}
}
