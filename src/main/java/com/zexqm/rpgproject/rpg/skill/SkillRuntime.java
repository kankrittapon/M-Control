package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.ManaProvider;
import com.zexqm.rpgproject.rpg.RpgPlayerData;
import com.zexqm.rpgproject.rpg.RpgPlayerDataProvider;
import com.zexqm.rpgproject.rpg.combat.RpgCombatService;
import com.zexqm.rpgproject.rpg.combat.RpgCombatState;
import com.zexqm.rpgproject.rpg.combat.RpgCombatStateProvider;
import com.zexqm.rpgproject.rpg.combat.RpgDamageContext;
import com.zexqm.rpgproject.rpg.combat.SmashResolver;
import com.zexqm.rpgproject.rpg.status.RpgStatusService;
import com.zexqm.rpgproject.network.RpgNetwork;
import com.zexqm.rpgproject.network.SyncManaPacket;
import com.zexqm.rpgproject.network.SyncPlayerFacingPacket;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SkillRuntime {
    private static final Map<UUID, ActiveCast> ACTIVE = new HashMap<>();
    private static final Map<UUID, PendingCast> PENDING = new HashMap<>();
    private static final Map<UUID, SkillLinkState> LINKS = new HashMap<>();

    public static SkillCastResult cast(ServerPlayer player, ResourceLocation skillId, SkillExecutionContext context) {
        RpgProject.LOGGER.info("[RPG Skill] request player={} skill={} direction={} target={} ground={}",
                player.getScoreboardName(), skillId, context.direction(), context.targetEntityId(), context.groundPosition());
        RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
        RpgCombatState combat = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (data == null || combat == null) return result(player, skillId, SkillCastResult.INVALID_STATE, "capability missing");
        SkillDefinition firstRank = SkillRegistry.get(skillId);
        boolean debug = firstRank != null && firstRank.debugOnly();
        int progressionRank = data.skillProgress().rank(skillId);
        int learnedRank = debug ? firstRank.rank()
                : firstRank != null && firstRank.innate() ? Math.max(firstRank.rank(), progressionRank)
                : progressionRank;
        SkillDefinition skill = SkillRegistry.get(skillId, learnedRank);
        if (skill == null) return result(player, skillId, learnedRank <= 0
                ? SkillCastResult.NOT_LEARNED : SkillCastResult.UNKNOWN_SKILL, "rank=" + learnedRank + " not registered");
        if (skill.debugOnly() && !player.hasPermissions(2))
            return result(player, skillId, SkillCastResult.DEBUG_FORBIDDEN, "permission<2");
        if (combat.actionLocked() || PENDING.containsKey(player.getUUID()))
            return result(player, skillId, SkillCastResult.INVALID_STATE, "lock/active");
        boolean transitioning = ACTIVE.containsKey(player.getUUID());
        if (data.rpgClass() != skill.rpgClass())
            return result(player, skillId, SkillCastResult.WRONG_CLASS, "player=" + data.rpgClass());
        if (skill.specialization() != null && data.specialization() != skill.specialization())
            return result(player, skillId, SkillCastResult.WRONG_SPECIALIZATION, "player=" + data.specialization());
        if (!skill.weapons().test(data))
            return result(player, skillId, SkillCastResult.WRONG_WEAPON, "requirements=" + skill.weapons());
        if (PrimaryResourceType.forClass(data.rpgClass()) != skill.resourceType())
            return result(player, skillId, SkillCastResult.WRONG_RESOURCE, "required=" + skill.resourceType());
        boolean needsAutoDraw = !transitioning && data.actionState() == SkillActionState.SHEATHED;
        if (!transitioning && !needsAutoDraw && data.actionState() != SkillActionState.READY)
            return result(player, skillId, SkillCastResult.INVALID_STATE, "action=" + data.actionState());
        long gameTime = player.serverLevel().getServer().overworld().getGameTime();
        SkillLinkState linkState = LINKS.computeIfAbsent(player.getUUID(), ignored -> new SkillLinkState());
        if (!linkState.has(skill.links().requires(), gameTime))
            return result(player, skillId, SkillCastResult.MISSING_SKILL_LINK,
                    "required=" + skill.links().requires());
        if (skill.links().useRequiredAnchor()) {
            var anchor = linkState.anchor(skill.links().requires(), gameTime);
            if (anchor == null)
                return result(player, skillId, SkillCastResult.INVALID_TARGET,
                        "required link has no resolved impact anchor");
            context = new SkillExecutionContext(player, player.getEyePosition(), context.direction(),
                    context.targetEntityId(), anchor, context.lateralSide());
        }
        boolean cooldownRecast = !data.cooldownReady(skill.id(), gameTime);
        if (cooldownRecast && !skill.cooldownRecast().enabled())
            return result(player, skillId, SkillCastResult.COOLDOWN,
                    "remaining=" + data.cooldownRemaining(skill.id(), gameTime));
        var mana = player.getCapability(ManaProvider.MANA).orElse(null);
        if (mana == null || mana.getMana() < skill.resourceCost() || data.stamina() < skill.staminaCost())
            return result(player, skillId, SkillCastResult.INSUFFICIENT_RESOURCE,
                    "primary=" + (mana == null ? "missing" : mana.getMana()) + " stamina=" + data.stamina());
        // A required impact anchor was produced and range-validated by the server-side source skill.
        // Revalidating it from the caster's current position would make movement invalidate follow-ups.
        if (!skill.links().useRequiredAnchor() && !validTarget(skill, context))
            return result(player, skillId, SkillCastResult.INVALID_TARGET, "range/direction/entity");
        if (needsAutoDraw) {
            if (!data.requestToggleDraw(SkillRuntimeConfig.values().drawTicks(),
                    SkillRuntimeConfig.values().sheatheTicks()))
                return result(player, skillId, SkillCastResult.INVALID_STATE, "draw rejected");
            PENDING.put(player.getUUID(), new PendingCast(skillId, context));
            return result(player, skillId, SkillCastResult.STARTED, "queued auto-draw after validation");
        }
        if (transitioning && !transitionTo(player, skill))
            return result(player, skillId, SkillCastResult.INVALID_STATE, "transition rejected");

        mana.spend(skill.resourceCost());
        int castRecovery = mana.restore((int) Math.floor(
                mana.getMaxMana() * skill.castMpRecoveryPercent() + 1.0e-9));
        if (castRecovery > 0) {
            RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncManaPacket(mana.getMana(), mana.getMaxMana()));
            RpgProject.LOGGER.info("[RPG Skill] cast-resource player={} skill={} recovered={} current={} max={}",
                    player.getScoreboardName(), skill.id(), castRecovery, mana.getMana(), mana.getMaxMana());
        }
        data.spendStamina(skill.staminaCost());
        if (skill.links().consumeRequiredOnCastStart()) {
            linkState.consume(skill.links().requires(), gameTime);
            RpgProject.LOGGER.info("[RPG Skill] link-consume player={} skill={} link={}",
                    player.getScoreboardName(), skill.id(), skill.links().requires());
        }
        grantLink(skill, SkillLinkTiming.CAST_START, player, gameTime);
        if (!cooldownRecast) data.startCooldown(skill.id(), gameTime + skill.cooldownTicks());
        data.startCast(skill.castTicks());
        combat.setCasting(true);
        ACTIVE.put(player.getUUID(), new ActiveCast(skill, context, cooldownRecast, 0, false, 0, false));
        if (skill.facingPolicy() == FacingPolicy.AIM_ON_CAST
                || skill.facingPolicy() == FacingPolicy.TARGET_ON_CAST) {
            updateFacing(player, skill, context);
        }
        return result(player, skillId, SkillCastResult.STARTED, "castTicks=" + skill.castTicks()
                + " recoveryTicks=" + skill.recoveryTicks() + " mpCost=" + skill.resourceCost()
                + " staminaCost=" + skill.staminaCost() + " cooldownRecast=" + cooldownRecast);
    }

    public static boolean tick(ServerPlayer player) {
        boolean projectileActive = SkillProjectileRuntime.tick(player);
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
                        pending.context.direction(), pending.context.targetEntityId(), pending.context.groundPosition(),
                        pending.context.lateralSide());
                cast(player, pending.skillId, refreshed);
                return true;
            }
            return true;
        }
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active == null) return projectileActive;
        active.elapsedTicks++;
        int timeoutTicks = active.skill.castTicks() + active.skill.recoveryTicks() + 100;
        if (active.elapsedTicks > timeoutTicks) {
            RpgProject.LOGGER.warn("[RPG Skill] Force-cleared stale cast player={} skill={} elapsed={} timeout={}",
                    player.getUUID(), active.skill.id(), active.elapsedTicks, timeoutTicks);
            cancel(player);
            return true;
        }
        RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
        RpgCombatState combat = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (data == null || combat == null || combat.actionLocked()) {
            cancel(player);
            return true;
        }
        if (!active.recovering) {
            if (!active.movementApplied) {
                active.context = applyCasterMovement(player, active.skill, active.context);
                active.movementApplied = true;
            }
            if (active.skill.facingPolicy() != FacingPolicy.NONE
                    && active.tick <= firstHitTick(active.skill)) {
                updateFacing(player, active.skill, active.context);
            }
            if (!active.cooldownRecast || active.skill.cooldownRecast().allowProtection())
                applyProtection(active.skill, active.tick, combat, player);
            for (SkillDefinition.Hit hit : active.skill.hits()) {
                if (hit.timingTick() == active.tick) {
                    if (active.skill.targeting() == SkillTargetingType.AIM_PROJECTILE)
                        SkillProjectileRuntime.spawn(active.skill, hit, active.context,
                                active.cooldownRecast, gameTime(player));
                    else executeHit(active.skill, hit, active.context, active.cooldownRecast, gameTime(player));
                }
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
            grantLink(active.skill, SkillLinkTiming.CAST_COMPLETE, player, gameTime(player));
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

    private static SkillExecutionContext applyCasterMovement(ServerPlayer player, SkillDefinition skill,
                                                              SkillExecutionContext context) {
        if (skill.casterMovementType() != CasterMovementType.SIDE_HOP
                || skill.casterLateralDistance() <= 0) return context;
        int side = context.lateralSide();
        if (side == 0) {
            RpgProject.LOGGER.info("[RPG Skill] side-hop skipped player={} skill={} reason=missing_direction",
                    player.getScoreboardName(), skill.id());
            return context;
        }
        Vec3 forward = context.direction().multiply(1, 0, 1);
        if (forward.lengthSqr() <= 1.0e-6) return context;
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 before = player.position();
        Vec3 requested = right.scale(skill.casterLateralDistance() * side);
        player.move(net.minecraft.world.entity.MoverType.SELF, requested);
        Vec3 actual = player.position().subtract(before);
        player.setDeltaMovement(0, Math.max(player.getDeltaMovement().y, 0.12), 0);
        player.hurtMarked = true;
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        RpgProject.LOGGER.info(
                "[RPG Skill] side-hop player={} skill={} side={} requested={} actual={} collisionClipped={}",
                player.getScoreboardName(), skill.id(), side, skill.casterLateralDistance(),
                actual.horizontalDistance(), actual.horizontalDistance() + 1.0e-3 < requested.horizontalDistance());
        return new SkillExecutionContext(player, player.getEyePosition(), context.direction(),
                context.targetEntityId(), context.groundPosition(), side);
    }

    public static boolean requestCancel(ServerPlayer player, SkillCancelTrigger trigger) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active == null) return true;
        if (!active.skill.transitions().allows(trigger, active.tick, firstHitTick(active.skill))) {
            RpgProject.LOGGER.info("[RPG Skill] cancel-rejected player={} skill={} trigger={} tick={}",
                    player.getScoreboardName(), active.skill.id(), trigger, active.tick);
            return false;
        }
        ResourceLocation id = active.skill.id();
        cancel(player);
        RpgProject.LOGGER.info("[RPG Skill] cancel-accepted player={} skill={} trigger={}",
                player.getScoreboardName(), id, trigger);
        return true;
    }

    public static boolean movementCancelAllowed(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        return active != null && active.skill.transitions().allows(SkillCancelTrigger.MOVEMENT,
                active.tick, firstHitTick(active.skill));
    }

    private static boolean transitionTo(ServerPlayer player, SkillDefinition incoming) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active == null) return true;
        if (!incoming.transitions().interruptsCasting()
                || !active.skill.transitions().allows(SkillCancelTrigger.SKILL,
                active.tick, firstHitTick(active.skill))) return false;
        return requestCancel(player, SkillCancelTrigger.SKILL);
    }

    private static int firstHitTick(SkillDefinition skill) {
        return skill.hits().stream().mapToInt(SkillDefinition.Hit::timingTick).min()
                .orElse(skill.castTicks() + 1);
    }

    private static void updateFacing(ServerPlayer player, SkillDefinition skill, SkillExecutionContext context) {
        net.minecraft.world.phys.Vec3 direction = context.direction();
        if (skill.facingPolicy() == FacingPolicy.TARGET_ON_CAST && context.targetEntityId() != null) {
            var target = player.serverLevel().getEntity(context.targetEntityId());
            if (target != null) direction = target.position().subtract(player.position());
        }
        if (direction.horizontalDistanceSqr() < 1.0E-6D) return;
        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float yaw = skill.turnSpeed() <= 0
                ? targetYaw
                : net.minecraft.util.Mth.approachDegrees(player.getYRot(), targetYaw, (float) skill.turnSpeed());
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerFacingPacket(yaw));
    }

    public static void clearTransient(ServerPlayer player) {
        cancel(player);
        SkillProjectileRuntime.clear(player);
        SkillLinkState links = LINKS.remove(player.getUUID());
        if (links != null) links.clear();
    }

    public static MovementPolicy movementPolicy(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        return active == null ? MovementPolicy.FULL : active.skill.movementPolicy();
    }

    public static ResourceLocation activeSkillId(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        return active == null ? null : active.skill.id();
    }

    public static int activeCastTicks(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        return active == null ? 0 : active.skill.castTicks();
    }

    private static void executeHit(SkillDefinition skill, SkillDefinition.Hit hit,
                                   SkillExecutionContext context, boolean cooldownRecast, long gameTime) {
        var resolution = SkillHitResolver.resolveMeasured(skill, hit, context);
        var targets = resolution.targets();
        RpgProject.LOGGER.info("[RPG Skill] hit-window player={} skill={} tick={} shape={} targets={}",
                context.caster().getScoreboardName(), skill.id(), hit.timingTick(), skill.targeting(), targets.size());
        var observability = SkillRuntimeConfig.values().observability();
        if (observability.logHitPerformance()) {
            long micros = resolution.elapsedNanos() / 1_000;
            RpgProject.LOGGER.info("[RPG Skill Perf] skill={} tick={} candidates={} accepted={} resolverMicros={} slow={}",
                    skill.id(), hit.timingTick(), resolution.candidateCount(), targets.size(), micros,
                    micros >= observability.slowResolverMicros());
        }
        SkillDebugVisualizer.show(skill, hit, context, targets);
        executeResolvedHit(skill, hit, context, cooldownRecast, gameTime, targets);
        if (skill.links().grants() != null
                && skill.links().grantTiming() == SkillLinkTiming.SUCCESSFUL_HIT) {
            Vec3 anchor = context.groundPosition() != null
                    ? context.groundPosition()
                    : context.origin().add(context.direction().scale(skill.range()));
            recordLinkAnchor(skill, context.caster(), anchor);
        }
    }

    static void executeResolvedHit(SkillDefinition skill, SkillDefinition.Hit hit,
                                   SkillExecutionContext context, boolean cooldownRecast, long gameTime,
                                   Set<net.minecraft.world.entity.LivingEntity> targets) {
        SkillDefinition.CooldownRecastPolicy recast = skill.cooldownRecast();
        boolean recoveryApplied = false;
        boolean successfulHit = false;
        int targetIndex = 0;
        for (var target : targets) {
            double targetMultiplier = hit.targetDamageMultiplier(targetIndex++);
            boolean damaging = hit.baseDamage() > 0
                    || hit.powerType() != com.zexqm.rpgproject.rpg.combat.RpgPowerType.NONE
                    && hit.coefficient() > 0;
            if (damaging) {
                var crowdControl = target instanceof net.minecraft.world.entity.player.Player
                        && hit.playerCrowdControl() != null ? hit.playerCrowdControl() : hit.crowdControl();
                var result = RpgCombatService.apply(new RpgDamageContext(context.caster(), target, context.origin(),
                        hit.baseDamage(), hit.powerType(), hit.coefficient() * targetMultiplier * (cooldownRecast
                        ? recast.damageMultiplier() : 1.0), true,
                        !cooldownRecast || recast.allowCritical(),
                        hit.hitChanceBonus(), hit.criticalChanceBonus(),
                        cooldownRecast && !recast.allowCrowdControl() ? null : crowdControl,
                        cooldownRecast && !recast.allowSpecialAttacks() ? java.util.Set.of() : hit.specialAttacks()));
                if (SkillRuntimeConfig.values().observability().logPerTargetResults())
                    RpgProject.LOGGER.info("[RPG Skill] damage skill={} target={}#{} targetMultiplier={} outcome={} damage={} specials={} cc={}",
                            skill.id(), target.getType().toShortString(), target.getId(), targetMultiplier,
                            result.outcome(), result.damage(), result.specialAttacks(), result.crowdControl());
                if (result.outcome() != com.zexqm.rpgproject.rpg.combat.RpgDamageResult.Outcome.HIT) continue;
            }
            successfulHit = true;
            if (hit.defensive().active()) {
                target.getCapability(RpgCombatStateProvider.DATA).ifPresent(state -> {
                    state.activateManaShield(hit.defensive().manaShieldTicks(), hit.defensive().manaShieldRatio());
                    state.activateResistanceBuff(hit.defensive().resistanceTicks(), hit.defensive().resistanceBonus());
                });
                RpgProject.LOGGER.info("[RPG Skill] defensive skill={} target={}#{} manaShield={} shieldTicks={} resistance={} resistanceTicks={}",
                        skill.id(), target.getType().toShortString(), target.getId(),
                        hit.defensive().manaShieldRatio(), hit.defensive().manaShieldTicks(),
                        hit.defensive().resistanceBonus(), hit.defensive().resistanceTicks());
            }
            if (hit.health().active()) {
                boolean casterTarget = target == context.caster();
                float before = target.getHealth();
                float requested = (float) (target.getMaxHealth() * hit.health().recoveryPercent(casterTarget)
                        + hit.health().flatRecovery(casterTarget));
                target.heal(requested);
                float recovered = target.getHealth() - before;
                if (SkillRuntimeConfig.values().observability().logPerTargetResults())
                    RpgProject.LOGGER.info("[RPG Skill] heal skill={} target={}#{} requested={} recovered={} health={}/{}",
                            skill.id(), target.getType().toShortString(), target.getId(), requested, recovered,
                            target.getHealth(), target.getMaxHealth());
            }
            if (hit.pullStrength() > 0 && !(target instanceof net.minecraft.world.entity.player.Player)
                    && !com.zexqm.rpgproject.rpg.mob.MobControlProfiles.resolve(target).hardCcImmune()) {
                net.minecraft.world.phys.Vec3 towardCaster = context.caster().position()
                        .subtract(target.position()).multiply(1, 0, 1);
                if (towardCaster.lengthSqr() > 1.0e-6) {
                    net.minecraft.world.phys.Vec3 impulse = towardCaster.normalize().scale(hit.pullStrength());
                    target.push(impulse.x, 0.04, impulse.z);
                    target.hurtMarked = true;
                }
            }
            var casterMana = context.caster().getCapability(ManaProvider.MANA).orElse(null);
            var targetMana = target.getCapability(ManaProvider.MANA).orElse(null);
            boolean allowRecovery = !hit.resources().recoverOncePerHitWindow() || !recoveryApplied;
            if (casterMana != null && (!cooldownRecast || recast.allowResources())) {
                var resourceResult = SkillResourceService.apply(casterMana, targetMana, hit.resources(), allowRecovery);
                if (resourceResult.recovered() > 0) recoveryApplied = true;
                if (resourceResult.recovered() > 0)
                    RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> context.caster()),
                            new SyncManaPacket(casterMana.getMana(), casterMana.getMaxMana()));
                if (resourceResult.drained() > 0 && target instanceof ServerPlayer targetPlayer)
                    RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> targetPlayer),
                            new SyncManaPacket(targetMana.getMana(), targetMana.getMaxMana()));
                if (resourceResult.status() != ResourceTransactionResult.Status.NO_EFFECT)
                    if (SkillRuntimeConfig.values().observability().logPerTargetResults())
                        RpgProject.LOGGER.info("[RPG Skill] resource skill={} target={}#{} result={} recovered={} drained={} transferred={}",
                            skill.id(), target.getType().toShortString(), target.getId(), resourceResult.status(),
                            resourceResult.recovered(), resourceResult.drained(), resourceResult.transferred());
            }
            for (SkillDefinition.StatusPayload status : cooldownRecast && !recast.allowStatuses()
                    ? java.util.List.<SkillDefinition.StatusPayload>of() : hit.statuses()) {
                var statusResult = RpgStatusService.apply(context.caster(), target, status.type(), status.durationTicks(),
                        status.intervalTicks(), status.potency(), status.maxStacks(), status.stacking(),
                        status.allowedProfiles());
                if (SkillRuntimeConfig.values().observability().logPerTargetResults())
                    RpgProject.LOGGER.info("[RPG Skill] status skill={} target={}#{} type={} result={} duration={} potency={}",
                        skill.id(), target.getType().toShortString(), target.getId(), status.type(), statusResult,
                        status.durationTicks(), status.potency());
            }
            for (SkillDefinition.SmashPayload smash : cooldownRecast && !recast.allowSmash()
                    ? java.util.List.<SkillDefinition.SmashPayload>of() : hit.smashes()) {
                var smashResult = SmashResolver.apply(target, smash.type(), smash.chance(), context.origin());
                if (SkillRuntimeConfig.values().observability().logPerTargetResults())
                    RpgProject.LOGGER.info("[RPG Skill] smash skill={} target={}#{} type={} chance={} result={} extension={}",
                        skill.id(), target.getType().toShortString(), target.getId(), smash.type(), smash.chance(),
                        smashResult.status(), smashResult.extensionTicks());
            }
        }
        if (successfulHit) grantLink(skill, SkillLinkTiming.SUCCESSFUL_HIT, context.caster(), gameTime);
    }

    private static void grantLink(SkillDefinition skill, SkillLinkTiming timing,
                                  ServerPlayer player, long gameTime) {
        var policy = skill.links();
        if (policy.grants() == null || policy.grantTiming() != timing) return;
        long expiration = gameTime + policy.grantDurationTicks();
        LINKS.computeIfAbsent(player.getUUID(), ignored -> new SkillLinkState())
                .grant(policy.grants(), expiration);
        RpgProject.LOGGER.info("[RPG Skill] link-grant player={} skill={} link={} timing={} duration={} expiration={}",
                player.getScoreboardName(), skill.id(), policy.grants(), timing,
                policy.grantDurationTicks(), expiration);
    }

    static void recordLinkAnchor(SkillDefinition skill, ServerPlayer player, net.minecraft.world.phys.Vec3 anchor) {
        ResourceLocation link = skill.links().grants();
        if (link == null) return;
        LINKS.computeIfAbsent(player.getUUID(), ignored -> new SkillLinkState()).setAnchor(link, anchor);
        RpgProject.LOGGER.info("[RPG Skill] link-anchor player={} skill={} link={} position={}",
                player.getScoreboardName(), skill.id(), link, anchor);
    }

    private static long gameTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
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
            boolean selfAllowed = skill.hits().stream().anyMatch(hit ->
                    hit.targetDisposition() == SkillTargetDisposition.SELF
                            || hit.targetDisposition() == SkillTargetDisposition.SELF_AND_ALLY);
            if (context.targetEntityId() == null) return selfAllowed;
            var entity = playerLevel(context).getEntity(context.targetEntityId());
            if (entity instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()
                    && validEntityTarget(skill, context.caster(), living)
                    && living.distanceToSqr(context.caster()) <= skill.range() * skill.range()) {
                net.minecraft.world.phys.Vec3 eyePos = context.caster().getEyePosition();
                net.minecraft.world.phys.Vec3 targetCenter = living.getBoundingBox().getCenter();
                net.minecraft.world.phys.HitResult los = context.caster().level().clip(new net.minecraft.world.level.ClipContext(
                    eyePos, targetCenter, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
                    context.caster()));
                return los.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || selfAllowed;
            }
            return selfAllowed;
        }
        if (skill.targeting() == SkillTargetingType.CHAIN) {
            if (context.targetEntityId() == null) return false;
            var entity = playerLevel(context).getEntity(context.targetEntityId());
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || !living.isAlive()
                    || living == context.caster()) return false;
            net.minecraft.world.phys.Vec3 targetDirection = living.getBoundingBox().getCenter()
                    .subtract(context.origin());
            if (targetDirection.lengthSqr() > skill.range() * skill.range()
                    || targetDirection.lengthSqr() < 1.0E-6D) return false;
            // Detached third-person camera rays are shoulder-offset from the server eye position.
            // Validate the transient hovered entity without creating selected-target fallback.
            return targetDirection.normalize().dot(context.direction()) >= 0.85D
                    && context.caster().hasLineOfSight(living);
        }
        if (skill.targeting() == SkillTargetingType.GROUND_AOE
                || skill.targeting() == SkillTargetingType.CIRCLE) {
            return context.groundPosition() != null
                    && context.groundPosition().distanceToSqr(context.origin()) <= skill.range() * skill.range();
        }
        return true;
    }

    private static boolean validEntityTarget(SkillDefinition skill, ServerPlayer caster,
                                             net.minecraft.world.entity.LivingEntity target) {
        SkillTargetDisposition disposition = skill.hits().isEmpty()
                ? SkillTargetDisposition.HOSTILE : skill.hits().get(0).targetDisposition();
        return switch (disposition) {
            case HOSTILE -> target != caster && !caster.isAlliedTo(target);
            case SELF -> target == caster;
            case ALLY -> target != caster && target instanceof net.minecraft.world.entity.player.Player;
            case SELF_AND_ALLY -> target == caster
                    || target instanceof net.minecraft.world.entity.player.Player;
        };
    }

    private static net.minecraft.server.level.ServerLevel playerLevel(SkillExecutionContext context) {
        return context.caster().serverLevel();
    }

    private static final class ActiveCast {
        private final SkillDefinition skill;
        private SkillExecutionContext context;
        private final boolean cooldownRecast;
        private int tick;
        private boolean recovering;
        private int elapsedTicks;
        private boolean movementApplied;

        private ActiveCast(SkillDefinition skill, SkillExecutionContext context, boolean cooldownRecast,
                           int tick, boolean recovering, int elapsedTicks, boolean movementApplied) {
            this.skill = skill;
            this.context = context;
            this.cooldownRecast = cooldownRecast;
            this.tick = tick;
            this.recovering = recovering;
            this.elapsedTicks = elapsedTicks;
            this.movementApplied = movementApplied;
        }
    }

    private record PendingCast(ResourceLocation skillId, SkillExecutionContext context) {}

    private SkillRuntime() {}
}
