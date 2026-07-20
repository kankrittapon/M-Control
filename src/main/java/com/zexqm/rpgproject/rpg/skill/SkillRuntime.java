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
    private static final Map<UUID, TargetingSession> TARGETING = new HashMap<>();
    private static final Map<UUID, SkillLinkState> LINKS = new HashMap<>();

    public static SkillCastResult beginTargeting(ServerPlayer player, ResourceLocation skillId) {
        RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
        RpgCombatState combat = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        SkillDefinition skill = learnedSkill(player, data, skillId);
        if (data == null || combat == null)
            return result(player, skillId, SkillCastResult.INVALID_STATE, "targeting capability missing");
        if (skill == null)
            return result(player, skillId, SkillCastResult.NOT_LEARNED, "targeting skill unavailable");
        if (skill.debugOnly() && !player.hasPermissions(2))
            return result(player, skillId, SkillCastResult.DEBUG_FORBIDDEN, "permission<2");
        if (skill.aimMode() != SkillAimMode.CONFIRM_TARGETING)
            return result(player, skillId, SkillCastResult.INVALID_STATE, "aim_mode=" + skill.aimMode());
        if (data.actionState() != SkillActionState.READY || combat.actionLocked()
                || ACTIVE.containsKey(player.getUUID()) || PENDING.containsKey(player.getUUID())
                || TARGETING.containsKey(player.getUUID()))
            return result(player, skillId, SkillCastResult.INVALID_STATE, "cannot enter targeting");
        if (data.rpgClass() != skill.rpgClass())
            return result(player, skillId, SkillCastResult.WRONG_CLASS, "player=" + data.rpgClass());
        if (skill.specialization() != null && data.specialization() != skill.specialization())
            return result(player, skillId, SkillCastResult.WRONG_SPECIALIZATION,
                    "player=" + data.specialization());
        if (!skill.weapons().test(data))
            return result(player, skillId, SkillCastResult.WRONG_WEAPON,
                    "requirements=" + skill.weapons());
        if (PrimaryResourceType.forClass(data.rpgClass()) != skill.resourceType())
            return result(player, skillId, SkillCastResult.WRONG_RESOURCE,
                    "required=" + skill.resourceType());
        var mana = player.getCapability(ManaProvider.MANA).orElse(null);
        if (mana == null || mana.getMana() < skill.resourceCost() || data.stamina() < skill.staminaCost())
            return result(player, skillId, SkillCastResult.INSUFFICIENT_RESOURCE,
                    "targeting resource check");
        long now = gameTime(player);
        if (!data.cooldownReady(skill.id(), now) && !skill.cooldownRecast().enabled())
            return result(player, skillId, SkillCastResult.COOLDOWN,
                    "remaining=" + data.cooldownRemaining(skill.id(), now));
        long expiresAt = now + skill.targetingTimeoutTicks();
        TARGETING.put(player.getUUID(), new TargetingSession(skill, expiresAt));
        data.startTargeting(skill.targetingTimeoutTicks());
        RpgProject.LOGGER.info("[RPG Skill] targeting-start player={} skill={} expiresAt={}",
                player.getScoreboardName(), skill.id(), expiresAt);
        return result(player, skillId, SkillCastResult.STARTED, "targeting");
    }

    public static SkillCastResult confirmTargeting(ServerPlayer player, ResourceLocation skillId,
                                                    SkillExecutionContext context) {
        TargetingSession session = TARGETING.get(player.getUUID());
        if (session == null || !session.skill.id().equals(skillId))
            return result(player, skillId, SkillCastResult.INVALID_STATE, "no matching targeting session");
        if (gameTime(player) >= session.expiresAt)
            return result(player, skillId, SkillCastResult.INVALID_STATE, "targeting session expired");
        if (!validTarget(session.skill, context)) {
            Vec3 effectiveGround = session.skill.casterMovementType() == CasterMovementType.TELEPORT
                    && context.groundPosition() != null
                    ? clampTeleportGroundTarget(player, session.skill, context.groundPosition()) : null;
            RpgProject.LOGGER.info("[RPG Skill] targeting-invalid player={} skill={} rawGround={} effectiveGround={} safeLanding={}",
                    player.getScoreboardName(), skillId, context.groundPosition(), effectiveGround,
                    effectiveGround == null ? null : findSafeTeleportLanding(player, effectiveGround, 4.0));
            return result(player, skillId, SkillCastResult.INVALID_TARGET,
                    "target confirmation range/direction/entity");
        }
        TARGETING.remove(player.getUUID());
        player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(RpgPlayerData::cancelAction);
        RpgProject.LOGGER.info("[RPG Skill] targeting-confirm player={} skill={} ground={} target={}",
                player.getScoreboardName(), skillId, context.groundPosition(), context.targetEntityId());
        return cast(player, skillId, context, true);
    }

    public static boolean updateChannelTarget(ServerPlayer player, ResourceLocation skillId,
                                              SkillExecutionContext context) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active == null || active.recovering || !active.skill.id().equals(skillId)
                || active.skill.aimMode() != SkillAimMode.CHANNEL_TARGETING
                || !validTarget(active.skill, context)) {
            RpgProject.LOGGER.info("[RPG Skill] retarget-rejected player={} skill={}",
                    player.getScoreboardName(), skillId);
            return false;
        }
        active.context = context;
        RpgProject.LOGGER.info("[RPG Skill] retarget player={} skill={} tick={} ground={} target={}",
                player.getScoreboardName(), skillId, active.tick + active.castTimeOffset,
                context.groundPosition(), context.targetEntityId());
        return true;
    }

    public static SkillCastResult cast(ServerPlayer player, ResourceLocation skillId,
                                       SkillExecutionContext context) {
        return cast(player, skillId, context, false);
    }

    private static SkillCastResult cast(ServerPlayer player, ResourceLocation skillId,
                                        SkillExecutionContext context, boolean targetingConfirmed) {
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
        boolean directionalTeleport = skill.casterMovementType() == CasterMovementType.TELEPORT
                && context.forwardAxis() != 0;
        if (skill.aimMode() == SkillAimMode.CONFIRM_TARGETING
                && !targetingConfirmed && !directionalTeleport)
            return result(player, skillId, SkillCastResult.INVALID_STATE, "target confirmation required");
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
                    context.targetEntityId(), anchor, context.lateralSide(), context.forwardAxis());
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
            PENDING.put(player.getUUID(), new PendingCast(skillId, context, targetingConfirmed));
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
        int castTimeOffset = castTimeOffset(skill, combat);
        int effectiveCastTicks = Math.max(0, skill.castTicks() - castTimeOffset);
        data.startCast(effectiveCastTicks);
        combat.setCasting(true);
        ACTIVE.put(player.getUUID(), new ActiveCast(skill, context, cooldownRecast, 0, false, 0, false,
                castTimeOffset, effectiveCastTicks));
        if (skill.facingPolicy() == FacingPolicy.AIM_ON_CAST
                || skill.facingPolicy() == FacingPolicy.TARGET_ON_CAST) {
            updateFacing(player, skill, context);
        }
        return result(player, skillId, SkillCastResult.STARTED, "castTicks=" + effectiveCastTicks
                + " recoveryTicks=" + skill.recoveryTicks() + " mpCost=" + skill.resourceCost()
                + " staminaCost=" + skill.staminaCost() + " cooldownRecast=" + cooldownRecast
                + " castTimeOffset=" + castTimeOffset);
    }

    public static boolean tick(ServerPlayer player) {
        TauntBeaconService.tick(player);
        boolean projectileActive = SkillProjectileRuntime.tick(player);
        TargetingSession targeting = TARGETING.get(player.getUUID());
        if (targeting != null) {
            RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
            RpgCombatState combat = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
            if (data == null || combat == null || data.actionState() != SkillActionState.TARGETING
                    || combat.actionLocked()
                    || gameTime(player) >= targeting.expiresAt) {
                TARGETING.remove(player.getUUID());
                // A stance transition (for example Tab -> SHEATHING) owns its own next state.
                if (data != null && data.actionState() == SkillActionState.TARGETING)
                    data.cancelAction();
                RpgProject.LOGGER.info("[RPG Skill] targeting-cancel player={} skill={} reason={}",
                        player.getScoreboardName(), targeting.skill.id(),
                        combat != null && combat.actionLocked() ? "action_lock" : "timeout/state");
            }
            return true;
        }
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
                        pending.context.lateralSide(), pending.context.forwardAxis());
                cast(player, pending.skillId, refreshed, pending.targetingConfirmed);
                return true;
            }
            return true;
        }
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active == null) return projectileActive;
        active.elapsedTicks++;
        int timeoutTicks = active.effectiveCastTicks + active.skill.recoveryTicks() + 100;
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
            int logicalTick = active.tick + active.castTimeOffset;
            if (active.skill.facingPolicy() != FacingPolicy.NONE
                    && logicalTick <= firstHitTick(active.skill)) {
                updateFacing(player, active.skill, active.context);
            }
            if (!active.cooldownRecast || active.skill.cooldownRecast().allowProtection())
                applyProtection(active.skill, logicalTick, combat, player);
            for (SkillDefinition.Hit hit : active.skill.hits()) {
                if (hit.timingTick() == logicalTick) {
                    if (active.skill.targeting() == SkillTargetingType.AIM_PROJECTILE)
                        SkillProjectileRuntime.spawn(active.skill, hit, active.context,
                                active.cooldownRecast, gameTime(player));
                    else executeHit(active.skill, hit, active.context, active.cooldownRecast, gameTime(player));
                }
            }
            active.tick++;
            if (active.tick > active.effectiveCastTicks) {
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
        TargetingSession targeting = TARGETING.remove(player.getUUID());
        if (active != null || pending != null || targeting != null) RpgProject.LOGGER.info(
                "[RPG Skill] cancel player={} skill={}", player.getScoreboardName(),
                active != null ? active.skill.id() : pending != null ? pending.skillId() : targeting.skill.id());
        player.getCapability(RpgCombatStateProvider.DATA).ifPresent(state -> state.setCasting(false));
        player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(RpgPlayerData::cancelAction);
    }

    private static SkillExecutionContext applyCasterMovement(ServerPlayer player, SkillDefinition skill,
                                                              SkillExecutionContext context) {
        if (skill.casterMovementType() == CasterMovementType.NONE
                || skill.casterLateralDistance() <= 0) return context;
        int side = context.lateralSide();
        int forwardAxis = context.forwardAxis();
        if (skill.casterMovementType() == CasterMovementType.SIDE_HOP && side == 0) {
            RpgProject.LOGGER.info("[RPG Skill] side-hop skipped player={} skill={} reason=missing_direction",
                    player.getScoreboardName(), skill.id());
            return context;
        }
        Vec3 forward = context.direction().multiply(1, 0, 1);
        if (forward.lengthSqr() <= 1.0e-6) return context;
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        if (skill.casterMovementType() == CasterMovementType.TELEPORT)
            return applyTeleport(player, skill, context, forward.normalize());
        Vec3 moveDirection = skill.casterMovementType() == CasterMovementType.OMNI_DODGE
                ? forward.normalize().scale(forwardAxis).add(right.scale(side)) : right.scale(side);
        if (moveDirection.lengthSqr() <= 1.0e-6) moveDirection = forward.normalize();
        Vec3 before = player.position();
        Vec3 requested = moveDirection.normalize().scale(skill.casterLateralDistance());
        player.move(net.minecraft.world.entity.MoverType.SELF, requested);
        Vec3 actual = player.position().subtract(before);
        player.setDeltaMovement(0, Math.max(player.getDeltaMovement().y, 0.12), 0);
        player.hurtMarked = true;
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        RpgProject.LOGGER.info(
                "[RPG Skill] caster-move player={} skill={} type={} side={} forward={} requested={} actual={} collisionClipped={}",
                player.getScoreboardName(), skill.id(), skill.casterMovementType(), side, forwardAxis, skill.casterLateralDistance(),
                actual.horizontalDistance(), actual.horizontalDistance() + 1.0e-3 < requested.horizontalDistance());
        return new SkillExecutionContext(player, player.getEyePosition(), context.direction(),
                context.targetEntityId(), context.groundPosition(), side, forwardAxis);
    }

    private static SkillExecutionContext applyTeleport(ServerPlayer player, SkillDefinition skill,
                                                        SkillExecutionContext context, Vec3 forward) {
        int axis = context.forwardAxis();
        Vec3 requestedTarget;
        if (axis == 0 && context.groundPosition() != null) {
            Vec3 effectiveGround = clampTeleportGroundTarget(player, skill, context.groundPosition());
            requestedTarget = findSafeTeleportLanding(player, effectiveGround, 4.0);
            if (requestedTarget == null) {
                RpgProject.LOGGER.info("[RPG Skill] teleport-rejected player={} skill={} reason=no_safe_landing ground={}",
                        player.getScoreboardName(), skill.id(), effectiveGround);
                return context;
            }
        } else {
            requestedTarget = player.position().add(forward.scale(skill.casterLateralDistance()
                    * (axis < 0 ? -1 : 1)));
        }

        Vec3 start = player.position();
        Vec3 travel = requestedTarget.subtract(start);
        Vec3 eyeStart = player.getEyePosition();
        Vec3 traceEnd = axis == 0 && context.groundPosition() != null
                ? clampTeleportGroundTarget(player, skill, context.groundPosition())
                : requestedTarget.add(0, player.getEyeHeight(), 0);
        var hit = player.level().clip(new net.minecraft.world.level.ClipContext(eyeStart, traceEnd,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        boolean selectedSurface = axis == 0 && context.groundPosition() != null
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && hit.getLocation().distanceToSqr(clampTeleportGroundTarget(player, skill,
                context.groundPosition())) <= 0.25;
        boolean resolvedLedge = false;
        if (axis != 0 && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            Vec3 ledgeProbe = hit.getLocation().add(travel.normalize().scale(0.65))
                    .with(net.minecraft.core.Direction.Axis.Y, player.getY() + 4.0);
            Vec3 ledge = findSafeTeleportLanding(player, ledgeProbe, 4.0);
            if (ledge != null && ledge.y > player.getY() + 0.5
                    && ledge.subtract(start).multiply(1, 0, 1).lengthSqr()
                    <= skill.casterLateralDistance() * skill.casterLateralDistance()) {
                requestedTarget = ledge;
                travel = ledge.subtract(start);
                resolvedLedge = true;
            }
        }
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && !selectedSurface && !resolvedLedge) {
            double safeDistance = Math.max(0, hit.getLocation().distanceTo(eyeStart) - 0.65);
            travel = travel.normalize().scale(Math.min(travel.length(), safeDistance));
        }

        Vec3 destination = start.add(travel);
        Vec3 accepted = start;
        for (int step = 0; step <= 8; step++) {
            double scale = 1.0 - step * 0.125;
            Vec3 candidate = start.add(destination.subtract(start).scale(scale));
            if (player.level().noCollision(player, player.getBoundingBox().move(candidate.subtract(start)))) {
                accepted = candidate;
                break;
            }
        }
        player.connection.teleport(accepted.x, accepted.y, accepted.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        RpgProject.LOGGER.info(
                "[RPG Skill] teleport player={} skill={} axis={} requested={} actual={} collisionClipped={} ledge={}",
                player.getScoreboardName(), skill.id(), axis, start.distanceTo(requestedTarget),
                start.distanceTo(accepted), start.distanceTo(accepted) + 1.0e-3 < start.distanceTo(requestedTarget),
                resolvedLedge);
        return new SkillExecutionContext(player, player.getEyePosition(), context.direction(),
                context.targetEntityId(), context.groundPosition(), context.lateralSide(), axis);
    }

    private static Vec3 findSafeTeleportLanding(ServerPlayer player, Vec3 selectedSurface,
                                                 double maximumVerticalChange) {
        int minSupportY = net.minecraft.util.Mth.floor(player.getY() - maximumVerticalChange) - 1;
        int maxSupportY = net.minecraft.util.Mth.floor(player.getY() + maximumVerticalChange);
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int supportY = minSupportY; supportY <= maxSupportY; supportY++) {
            var support = new net.minecraft.core.BlockPos(
                    net.minecraft.util.Mth.floor(selectedSurface.x), supportY,
                    net.minecraft.util.Mth.floor(selectedSurface.z));
            var shape = player.level().getBlockState(support).getCollisionShape(player.level(), support);
            if (shape.isEmpty()) continue;
            double feetY = supportY + shape.max(net.minecraft.core.Direction.Axis.Y);
            if (Math.abs(feetY - player.getY()) > maximumVerticalChange + 1.0e-6) continue;
            Vec3 candidate = new Vec3(selectedSurface.x, feetY, selectedSurface.z);
            if (!player.level().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(player.position())))) continue;
            double distance = Math.abs(feetY - selectedSurface.y);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Vec3 clampTeleportGroundTarget(ServerPlayer player, SkillDefinition skill, Vec3 requested) {
        Vec3 origin = player.position();
        Vec3 horizontal = requested.subtract(origin).multiply(1, 0, 1);
        double maximum = skill.range();
        if (horizontal.lengthSqr() <= maximum * maximum || horizontal.lengthSqr() < 1.0e-6D) {
            return requested;
        }
        Vec3 clamped = horizontal.normalize().scale(maximum);
        return new Vec3(origin.x + clamped.x, requested.y, origin.z + clamped.z);
    }

    public static boolean requestCancel(ServerPlayer player, SkillCancelTrigger trigger) {
        if (TARGETING.containsKey(player.getUUID())) {
            cancel(player);
            return true;
        }
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active == null) return true;
        if (!active.skill.transitions().allows(trigger, active.tick + active.castTimeOffset,
                firstHitTick(active.skill))) {
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
                active.tick + active.castTimeOffset, firstHitTick(active.skill));
    }

    private static boolean transitionTo(ServerPlayer player, SkillDefinition incoming) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active == null) return true;
        if (!incoming.transitions().interruptsCasting()
                || !active.skill.transitions().allows(SkillCancelTrigger.SKILL,
                active.tick + active.castTimeOffset, firstHitTick(active.skill))) return false;
        return requestCancel(player, SkillCancelTrigger.SKILL);
    }

    private static int firstHitTick(SkillDefinition skill) {
        return skill.hits().stream().mapToInt(SkillDefinition.Hit::timingTick).min()
                .orElse(skill.castTicks() + 1);
    }

    static int castTimeOffset(SkillDefinition skill, RpgCombatState combat) {
        return combat != null && combat.ignoresCastTime()
                && skill.specialization() != com.zexqm.rpgproject.rpg.Specialization.AWAKENING
                ? Math.min(skill.castTicks(), firstHitTick(skill)) : 0;
    }

    private static void updateFacing(ServerPlayer player, SkillDefinition skill, SkillExecutionContext context) {
        net.minecraft.world.phys.Vec3 direction = context.direction();
        if (skill.casterMovementType() == CasterMovementType.TELEPORT
                && context.forwardAxis() == 0 && context.groundPosition() != null) {
            Vec3 destination = clampTeleportGroundTarget(player, skill, context.groundPosition());
            direction = destination.subtract(player.position());
        } else if (skill.facingPolicy() == FacingPolicy.TARGET_ON_CAST && context.targetEntityId() != null) {
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
        TauntBeaconService.clear(player);
        SkillLinkState links = LINKS.remove(player.getUUID());
        if (links != null) links.clear();
    }

    public static MovementPolicy movementPolicy(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        return active == null ? MovementPolicy.FULL : active.skill.movementPolicy();
    }

    public static ResourceLocation activeSkillId(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active != null) return active.skill.id();
        TargetingSession targeting = TARGETING.get(player.getUUID());
        return targeting == null ? null : targeting.skill.id();
    }

    public static SkillAimMode aimMode(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        if (active != null) return active.skill.aimMode();
        TargetingSession targeting = TARGETING.get(player.getUUID());
        return targeting == null ? SkillAimMode.INSTANT_AIM : targeting.skill.aimMode();
    }

    public static int activeCastTicks(ServerPlayer player) {
        ActiveCast active = ACTIVE.get(player.getUUID());
        return active == null ? 0 : active.effectiveCastTicks;
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
                    state.activateDamageReductionBuff(hit.defensive().damageReductionTicks(),
                            hit.defensive().damageReductionRatio());
                    state.activateSustainedResource(hit.defensive().sustainedResourceTicks(),
                            hit.defensive().resourceIntervalTicks(), hit.defensive().flatMpRecovery(),
                            hit.defensive().movementSpeedBonus());
                    state.activateSpeedBuff(hit.defensive().speedBuffTicks(),
                            hit.defensive().attackSpeedBonus(), hit.defensive().castingSpeedBonus(),
                            hit.defensive().timedMovementSpeedBonus());
                    state.activateCastTimeOverride(hit.defensive().castTimeOverrideTicks());
                });
                RpgProject.LOGGER.info("[RPG Skill] defensive skill={} target={}#{} manaShield={} shieldTicks={} resistance={} resistanceTicks={} damageReduction={} damageReductionTicks={} speedTicks={} attackSpeed={} castingSpeed={} movementSpeed={} castTimeOverrideTicks={}",
                        skill.id(), target.getType().toShortString(), target.getId(),
                        hit.defensive().manaShieldRatio(), hit.defensive().manaShieldTicks(),
                        hit.defensive().resistanceBonus(), hit.defensive().resistanceTicks(),
                        hit.defensive().damageReductionRatio(), hit.defensive().damageReductionTicks(),
                        hit.defensive().speedBuffTicks(), hit.defensive().attackSpeedBonus(),
                        hit.defensive().castingSpeedBonus(), hit.defensive().timedMovementSpeedBonus(),
                        hit.defensive().castTimeOverrideTicks());
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
        if (successfulHit && hit.tauntBeacon().active()
                && (!cooldownRecast || skill.cooldownRecast().allowResources()))
            TauntBeaconService.summon(context.caster(), hit.tauntBeacon(), context.direction());
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

    private static SkillDefinition learnedSkill(ServerPlayer player, RpgPlayerData data,
                                                ResourceLocation skillId) {
        if (data == null) return null;
        SkillDefinition firstRank = SkillRegistry.get(skillId);
        if (firstRank == null) return null;
        int progressionRank = data.skillProgress().rank(skillId);
        int learnedRank = firstRank.debugOnly() ? firstRank.rank()
                : firstRank.innate() ? Math.max(firstRank.rank(), progressionRank) : progressionRank;
        return SkillRegistry.get(skillId, learnedRank);
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
                case PVE_IFRAME -> state.activatePveIframe(2);
                case GRAB_IMMUNITY -> state.activateGrabImmunity(2);
            }
        }
    }

    private static boolean validTarget(SkillDefinition skill, SkillExecutionContext context) {
        if (context.direction().lengthSqr() < 0.99 || context.direction().lengthSqr() > 1.01) return false;
        if (skill.casterMovementType() == CasterMovementType.TELEPORT && context.forwardAxis() == 0) {
            if (context.groundPosition() == null) return false;
            Vec3 effectiveGround = clampTeleportGroundTarget(context.caster(), skill, context.groundPosition());
            return findSafeTeleportLanding(context.caster(), effectiveGround, 4.0) != null;
        }
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
        private final int castTimeOffset;
        private final int effectiveCastTicks;

        private ActiveCast(SkillDefinition skill, SkillExecutionContext context, boolean cooldownRecast,
                           int tick, boolean recovering, int elapsedTicks, boolean movementApplied,
                           int castTimeOffset, int effectiveCastTicks) {
            this.skill = skill;
            this.context = context;
            this.cooldownRecast = cooldownRecast;
            this.tick = tick;
            this.recovering = recovering;
            this.elapsedTicks = elapsedTicks;
            this.movementApplied = movementApplied;
            this.castTimeOffset = castTimeOffset;
            this.effectiveCastTicks = effectiveCastTicks;
        }
    }

    private record PendingCast(ResourceLocation skillId, SkillExecutionContext context,
                               boolean targetingConfirmed) {}
    private record TargetingSession(SkillDefinition skill, long expiresAt) {}

    private SkillRuntime() {}
}
