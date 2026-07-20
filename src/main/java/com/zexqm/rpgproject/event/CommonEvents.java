package com.zexqm.rpgproject.event;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.Mana;
import com.zexqm.rpgproject.mana.ManaProvider;
import com.zexqm.rpgproject.network.RpgNetwork;
import com.zexqm.rpgproject.network.SyncManaPacket;
import com.zexqm.rpgproject.network.SyncRpgDataPacket;
import com.zexqm.rpgproject.network.RequestClientAimCastPacket;
import net.minecraftforge.network.PacketDistributor;
import com.zexqm.rpgproject.rpg.RpgPlayerData;
import com.zexqm.rpgproject.rpg.RpgPlayerDataProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import com.zexqm.rpgproject.rpg.RpgClass;
import com.zexqm.rpgproject.rpg.Specialization;
import com.zexqm.rpgproject.rpg.DerivedStats;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import com.zexqm.rpgproject.registry.ModAttributes;
import com.zexqm.rpgproject.rpg.ClassProfileManager;
import com.zexqm.rpgproject.rpg.WeightCalculator;
import com.zexqm.rpgproject.rpg.combat.CombatConfig;
import com.zexqm.rpgproject.rpg.combat.CombatImpactCategory;
import com.zexqm.rpgproject.rpg.combat.CombatImpactContext;
import com.zexqm.rpgproject.rpg.combat.CombatImpactDiagnostics;
import com.zexqm.rpgproject.rpg.combat.CrowdControlResolver;
import com.zexqm.rpgproject.rpg.combat.RpgCombatService;
import com.zexqm.rpgproject.rpg.combat.RpgCombatState;
import com.zexqm.rpgproject.rpg.combat.RpgCombatStateProvider;
import com.zexqm.rpgproject.rpg.combat.RpgDamageContext;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.network.SyncCombatStatePacket;
import com.zexqm.rpgproject.network.SyncSkillStatePacket;
import com.zexqm.rpgproject.network.SyncEntityStatusesPacket;
import com.zexqm.rpgproject.rpg.skill.SkillRuntimeConfig;
import com.zexqm.rpgproject.rpg.skill.SkillProgressionConfig;
import com.zexqm.rpgproject.rpg.skill.SkillRegistry;
import com.zexqm.rpgproject.rpg.skill.SkillCatalog;
import com.zexqm.rpgproject.rpg.skill.SkillLearningService;
import com.zexqm.rpgproject.rpg.skill.SkillRuntime;
import com.zexqm.rpgproject.rpg.skill.SkillExecutionContext;
import com.zexqm.rpgproject.rpg.skill.MovementPolicy;
import com.zexqm.rpgproject.rpg.skill.PrimaryResourceType;
import com.zexqm.rpgproject.rpg.status.RpgStatusService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import com.zexqm.rpgproject.network.CommonPacketSync;
import net.minecraft.commands.CommandSourceStack;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID)
public final class CommonEvents {
    private static final UUID SPEED_SPELL_ATTACK_SPEED_ID =
            UUID.fromString("24f6067d-9aa4-4ac8-b5bd-6f69ec6ba206");
    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ClassProfileManager());
        event.addListener(new CombatConfig());
        event.addListener(new SkillRuntimeConfig());
        event.addListener(new SkillProgressionConfig());
        event.addListener(new SkillCatalog());
        event.addListener(new SkillRegistry());
    }

    @SubscribeEvent
    public static void syncDatapacks(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            syncSkillProgress(event.getPlayer());
            return;
        }
        event.getPlayerList().getPlayers().forEach(CommonEvents::syncSkillProgress);
    }

    @Mod.EventBusSubscriber(modid = RpgProject.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
            for (var attribute : java.util.List.of(ModAttributes.ATTACK_POWER, ModAttributes.MAGIC_POWER,
                    ModAttributes.DEFENSE, ModAttributes.DAMAGE_REDUCTION, ModAttributes.ACCURACY,
                    ModAttributes.EVASION, ModAttributes.CRITICAL_CHANCE, ModAttributes.CRITICAL_DAMAGE,
                    ModAttributes.CAST_SPEED, ModAttributes.CC_RESISTANCE)) {
                event.add(EntityType.PLAYER, attribute.get());
            }
        }
    }
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rpg")
                .then(Commands.literal("class").then(Commands.argument("value", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    RpgClass value = RpgClass.valueOf(StringArgumentType.getString(ctx, "value").toUpperCase());
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                        data.setClass(value); syncRpg(player, data);
                        CommonPacketSync.syncSkillProgress(player, data);
                    });
                    return 1;
                })))
                .then(Commands.literal("specialization").then(Commands.argument("value", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Specialization value = Specialization.valueOf(StringArgumentType.getString(ctx, "value").toUpperCase());
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                        data.setSpecialization(value); syncRpg(player, data);
                        CommonPacketSync.syncSkillProgress(player, data);
                    });
                    return 1;
                })))
                .then(Commands.literal("equip").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                        if (data.equip(player.getMainHandItem())) player.displayClientMessage(Component.literal("RPG weapon equipped."), false);
                        else player.displayClientMessage(Component.literal("This weapon does not match your class."), false);
                        syncRpg(player, data);
                    });
                    return 1;
                }))
                .then(Commands.literal("addxp").then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    long amount = LongArgumentType.getLong(ctx, "amount");
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                        int gained = data.addExperience(amount);
                        syncRpg(player, data);
                        CommonPacketSync.syncSkillProgress(player, data);
                        player.displayClientMessage(Component.literal("EXP +" + amount + " Levels gained=" + gained), false);
                    });
                    return 1;
                })))
                .then(Commands.literal("setlevel").then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
                    if (data == null) return 0;
                    int level = IntegerArgumentType.getInteger(ctx, "level");
                    data.setLevel(level);
                    syncRpg(player, data);
                    CommonPacketSync.syncSkillProgress(player, data);
                    return 1;
                })))
                .then(Commands.literal("addskillxp").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
                            if (data == null) return 0;
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            int gained = data.addSkillExperience(amount);
                            syncRpg(player, data);
                            CommonPacketSync.syncSkillProgress(player, data);
                            ctx.getSource().sendSuccess(() -> Component.literal("Skill EXP +" + amount
                                    + " SP gained=" + gained + " progress=" + data.skillExperience() + "/"
                                    + data.requiredSkillExperience()), false);
                            return 1;
                        })))
                .then(Commands.literal("protection").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String type = StringArgumentType.getString(ctx, "type").toLowerCase();
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    player.getCapability(RpgCombatStateProvider.DATA).ifPresent(state -> {
                                        switch (type) {
                                            case "fg" -> state.activateFrontGuard(ticks);
                                            case "sa" -> state.activateSuperArmor(ticks);
                                            case "iframe" -> state.activateIframe(ticks);
                                            case "grab_immune" -> state.activateGrabImmunity(ticks);
                                            default -> player.displayClientMessage(Component.literal("Use fg, sa, iframe, or grab_immune"), false);
                                        }
                                        syncCombat(player, state);
                                    });
                                    return 1;
                                }))))
                .then(Commands.literal("debug").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("perfect-guard")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                                        .executes(ctx -> debugPerfectGuard(ctx,
                                                IntegerArgumentType.getInteger(ctx, "ticks")))))
                        .then(Commands.literal("impact")
                                .then(Commands.argument("category", StringArgumentType.word())
                                        .then(Commands.argument("direction", StringArgumentType.word())
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1, 100000))
                                                        .executes(CommonEvents::debugImpact)))))
                        .then(Commands.literal("perf-spawn")
                                .then(Commands.argument("count", IntegerArgumentType.integer(10, 50))
                                        .executes(ctx -> spawnPerformanceTargets(ctx,
                                                IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("perf-clear")
                                .executes(CommonEvents::clearPerformanceTargets))
                        .then(Commands.literal("mana")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Mana mana = player.getCapability(ManaProvider.MANA).orElse(null);
                                    if (mana == null) return 0;
                                    int before = mana.getMana();
                                    mana.setMana(IntegerArgumentType.getInteger(ctx, "amount"));
                                    RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                            new SyncManaPacket(mana.getMana(), mana.getMaxMana()));
                                    RpgProject.LOGGER.info("[RPG Skill] debug-mana player={} before={} after={} max={}",
                                            player.getScoreboardName(), before, mana.getMana(), mana.getMaxMana());
                                    ctx.getSource().sendSuccess(() -> Component.literal("MP=" + mana.getMana()
                                            + "/" + mana.getMaxMana()), false);
                                    return 1;
                                })))
                        .then(Commands.literal("cc")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("type", StringArgumentType.word()).executes(ctx -> {
                                            Entity entity = EntityArgument.getEntity(ctx, "target");
                                            if (!(entity instanceof LivingEntity living)) return 0;
                                            CrowdControlType type = CrowdControlType.valueOf(
                                                    StringArgumentType.getString(ctx, "type").toUpperCase());
                                            var result = CrowdControlResolver.apply(living, type,
                                                    ctx.getSource().getPosition());
                                            ctx.getSource().sendSuccess(() -> Component.literal("CC result=" + result), false);
                                            if (living instanceof ServerPlayer targetPlayer) syncCombatState(targetPlayer);
                                            return result.status() == com.zexqm.rpgproject.rpg.combat.CrowdControlApplicationResult.Status.APPLIED ? 1 : 0;
                                        }))))
                        .then(Commands.literal("hit")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100000)).executes(ctx -> {
                                            ServerPlayer attacker = ctx.getSource().getPlayerOrException();
                                            Entity entity = EntityArgument.getEntity(ctx, "target");
                                            if (!(entity instanceof LivingEntity living)) return 0;
                                            var result = RpgCombatService.apply(RpgDamageContext.basic(attacker, living,
                                                    IntegerArgumentType.getInteger(ctx, "amount")));
                                            ctx.getSource().sendSuccess(() -> Component.literal("RPG hit result=" + result), false);
                                            return result.outcome() == com.zexqm.rpgproject.rpg.combat.RpgDamageResult.Outcome.HIT ? 1 : 0;
                                        }))))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .executes(CommonEvents::debugCast)))
                        .then(Commands.literal("aim-cast")
                                .then(Commands.argument("skill", ResourceLocationArgument.id()).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    var skillId = ResourceLocationArgument.getId(ctx, "skill");
                                    RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                            new RequestClientAimCastPacket(skillId));
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Requested client-camera aim for " + skillId), false);
                                    return 1;
                                }))))
                .then(Commands.literal("debug-ground").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("skill", ResourceLocationArgument.id())
                                .then(Commands.argument("distance", IntegerArgumentType.integer(1, 128))
                                        .executes(ctx -> debugGroundCast(ctx,
                                                IntegerArgumentType.getInteger(ctx, "distance"))))))
                .then(Commands.literal("skills")
                        .then(Commands.literal("list")
                                .executes(ctx -> listSkills(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> listSkills(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("skill", ResourceLocationArgument.id()).executes(ctx ->
                                        inspectSkill(ctx.getSource(), ResourceLocationArgument.getId(ctx, "skill")))))
                        .then(Commands.literal("upgrade")
                                .then(Commands.argument("skill", ResourceLocationArgument.id()).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    var data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
                                    if (data == null) return 0;
                                    var result = SkillLearningService.upgrade(data,
                                            ResourceLocationArgument.getId(ctx, "skill"));
                                    CommonPacketSync.syncSkillProgress(player, data);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Skill upgrade: " + result), false);
                                    return result.success() ? 1 : 0;
                                })))
                        .then(Commands.literal("force-upgrade").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .executes(ctx -> forceUpgradeSkill(ctx, null))
                                        .then(Commands.argument("rank", IntegerArgumentType.integer(1))
                                                .executes(ctx -> forceUpgradeSkill(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "rank"))))))
                        .then(Commands.literal("downgrade")
                                .then(Commands.argument("skill", ResourceLocationArgument.id()).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    var data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
                                    if (data == null) return 0;
                                    var result = SkillLearningService.downgrade(data,
                                            ResourceLocationArgument.getId(ctx, "skill"));
                                    CommonPacketSync.syncSkillProgress(player, data);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Skill downgrade: " + result), false);
                                    return result.success() ? 1 : 0;
                                })))
                        .then(Commands.literal("reset").requires(source -> source.hasPermission(2)).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                                SkillLearningService.reset(data);
                                CommonPacketSync.syncSkillProgress(player, data);
                            });
                            ctx.getSource().sendSuccess(() -> Component.literal("All learned skills reset."), false);
                            return 1;
                        })))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                        DerivedStats stats = data.stats();
                        player.displayClientMessage(Component.literal("Class=" + data.rpgClass() + " Spec=" + data.specialization()
                                + " Lv=" + data.level() + " EXP=" + data.experience() + "/" + data.requiredExperience()
                                + " SP=" + data.availableSkillPoints() + " (total=" + data.totalSkillPoints()
                                + " spent=" + data.spentSkillPoints() + ") SkillEXP=" + data.skillExperience()
                                + "/" + data.requiredSkillExperience() + " Drawn=" + data.weaponDrawn()
                                + " Action=" + data.actionState() + " Resource="
                                + PrimaryResourceType.forClass(data.rpgClass()) + " Cooldowns="
                                + data.cooldowns().size() + " Ready=" + data.canDraw()), false);
                        player.displayClientMessage(Component.literal(String.format("HP=%.0f MP=%.0f AP=%.1f Magic=%.1f DEF=%.1f DR=%.1f%% ACC=%.1f EVA=%.1f",
                                stats.maxHealth(), stats.maxMana(), stats.attackPower(), stats.magicPower(), stats.defense(),
                                stats.damageReduction() * 100.0, stats.accuracy(), stats.evasion())), false);
                        player.displayClientMessage(Component.literal(String.format("Stamina=%.1f/%.1f Breath=%d Strength=%d Health=%d Weight=%.1f/%.1f",
                                data.stamina(), data.maxStamina(), data.breath().level(), data.strength().level(),
                                data.healthTraining().level(), WeightCalculator.carried(player), WeightCalculator.capacity(data))), false);
                        player.getCapability(RpgCombatStateProvider.DATA).ifPresent(state ->
                                player.displayClientMessage(Component.literal(String.format(
                                        "Guard=%.1f/%.1f FG=%s(%d) SA=%s(%d) PerfectGuard=%s Iframe=%s(%d) CC=%.1f/%.1f Immune=%d Active=%s(%d) Down=%s Air=%s Frozen=%s Locked=%s Casting=%s ManaShield=%.0f%%(%d) ResistBuff=%.0f%%(%d) DamageReductionBuff=%.0f%%(%d) SustainedMP=%d/ticks=%d MoveSpeed=+%.0f%% SpeedBuff=%d ATK/CAST/MOVE=+%.0f/+%.0f/+%.0f%% InstantCast=%d",
                                        state.guard(), state.maximumGuard(), state.frontGuard(), state.frontGuardTicks(),
                                        state.superArmor(), state.superArmorTicks(),
                                        com.zexqm.rpgproject.rpg.combat.ProtectionResolver.perfectGuard(state),
                                        state.iframe(), state.iframeTicks(),
                                        state.ccPoints(), CombatConfig.values().maximumCcPoints(), state.ccImmunityTicks(),
                                        state.activeCc(), state.activeCcTicks(), state.downed(), state.floated(), state.frozen(),
                                        state.actionLocked(), state.casting(), state.manaShieldRatio() * 100.0,
                                        state.manaShieldTicks(), state.resistanceBuff() * 100.0,
                                        state.resistanceBuffTicks(), state.damageReductionBuff() * 100.0,
                                        state.damageReductionBuffTicks(), state.flatMpRecovery(),
                                        state.sustainedResourceTicks(), state.movementSpeedBonus() * 100.0,
                                        state.speedBuffTicks(), state.attackSpeedBonus() * 100.0,
                                        state.castingSpeedBonus() * 100.0,
                                        state.timedMovementSpeedBonus() * 100.0,
                                        state.castTimeOverrideTicks())), false));
                    });
                    return 1;
                })));
    }

    private static final String PERFORMANCE_TARGET_TAG = "rpg_project_perf_target";

    private static int forceUpgradeSkill(CommandContext<CommandSourceStack> context, Integer requestedRank)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        var data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
        if (data == null) return 0;
        ResourceLocation id = ResourceLocationArgument.getId(context, "skill");
        var catalog = SkillCatalog.get(id);
        if (catalog == null) {
            context.getSource().sendFailure(Component.literal("Unknown skill: " + id));
            return 0;
        }
        int targetRank = requestedRank == null
                ? data.skillProgress().rank(id) + 1 : requestedRank;
        if (targetRank > catalog.maximumRank()) {
            context.getSource().sendFailure(Component.literal("Requested rank " + targetRank
                    + " exceeds max rank " + catalog.maximumRank()));
            return 0;
        }
        if (data.skillProgress().rank(id) >= targetRank) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Acceptance rank already " + data.skillProgress().rank(id) + "/" + targetRank), false);
            return 1;
        }
        com.zexqm.rpgproject.rpg.skill.SkillLearningResult result = null;
        while (data.skillProgress().rank(id) < targetRank) {
            result = SkillLearningService.forceUpgradeForAcceptance(data, id);
            if (!result.success()) break;
        }
        CommonPacketSync.syncSkillProgress(player, data);
        int currentRank = data.skillProgress().rank(id);
        RpgProject.LOGGER.info("[RPG Skill] acceptance-force-upgrade player={} skill={} requestedRank={} currentRank={} result={}",
                player.getScoreboardName(), id, targetRank, currentRank, result);
        var finalResult = result;
        context.getSource().sendSuccess(() -> Component.literal(
                "Acceptance force-upgrade target=" + targetRank + " current=" + currentRank
                        + " result=" + finalResult), false);
        return currentRank == targetRank ? 1 : 0;
    }

    private static int debugCast(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "skill");
        try {
            var skill = SkillRegistry.get(id);
            Vec3 origin = player.getEyePosition();
            Vec3 direction = player.getLookAngle();
            net.minecraft.world.phys.HitResult aimed = player.pick(32.0, 0.0F, false);
            Vec3 rayEnd = origin.add(direction.scale(32.0));
            var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    player.serverLevel(), player, origin, rayEnd,
                    new net.minecraft.world.phys.AABB(origin, rayEnd).inflate(1.0),
                    entity -> entity instanceof LivingEntity living && living.isAlive()
                            && living != player, 32.0F * 32.0F);
            Integer targetId = entityHit == null ? null : entityHit.getEntity().getId();
            Vec3 ground = aimed instanceof net.minecraft.world.phys.BlockHitResult blockHit
                    ? blockHit.getLocation() : origin.add(direction.scale(8.0));
            if (skill != null && switch (skill.targeting()) {
                case RAY, AIM_PROJECTILE, LINE, CHAIN, ENTITY_TARGETED, GROUND_AOE, CIRCLE -> true;
                default -> false;
            }) {
                LivingEntity nearest = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(Math.max(1.0, skill.range())),
                                entity -> entity != player && entity.isAlive())
                        .stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                        .orElse(null);
                if (nearest != null) {
                    targetId = nearest.getId();
                    direction = nearest.getEyePosition().subtract(origin).normalize();
                    ground = nearest.position();
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Debug auto-aim: " + nearest.getType().toShortString()
                                    + " #" + nearest.getId() + " ground=" + nearest.position()), false);
                }
            }
            var result = SkillRuntime.cast(player, id, new SkillExecutionContext(player,
                    origin, direction, targetId, ground));
            context.getSource().sendSuccess(() -> Component.literal("Skill cast result=" + result), false);
            return result == com.zexqm.rpgproject.rpg.skill.SkillCastResult.STARTED ? 1 : 0;
        } catch (RuntimeException exception) {
            RpgProject.LOGGER.error("[RPG Skill] debug-cast-error player={} skill={}",
                    player.getScoreboardName(), id, exception);
            context.getSource().sendFailure(Component.literal("Debug cast failed for " + id + ": "
                    + exception.getClass().getSimpleName() + " - " + String.valueOf(exception.getMessage())));
            return 0;
        }
    }

    private static int spawnPerformanceTargets(CommandContext<CommandSourceStack> context, int count)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (count != 10 && count != 25 && count != 50) {
            context.getSource().sendFailure(Component.literal("Count must be 10, 25, or 50"));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        var level = player.serverLevel();
        clearPerformanceTargets(level);
        Vec3 forward = player.getLookAngle().multiply(1, 0, 1);
        if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0, 0, 1); else forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 origin = player.position().add(forward.scale(2.0));
        int columns = count == 10 ? 5 : count == 25 ? 5 : 10;
        int spawned = 0;
        for (int index = 0; index < count; index++) {
            Mob mob = EntityType.ZOMBIE.create(level);
            if (mob == null) continue;
            int row = index / columns;
            int column = index % columns;
            double lateral = (column - (columns - 1) * 0.5) * 0.7;
            double forwardOffset = row * 0.7;
            Vec3 position = origin.add(right.scale(lateral)).add(forward.scale(forwardOffset));
            mob.moveTo(position.x, position.y, position.z, player.getYRot() + 180.0F, 0.0F);
            mob.setNoAi(true);
            mob.setSilent(true);
            mob.setPersistenceRequired();
            mob.addTag(PERFORMANCE_TARGET_TAG);
            var maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) maxHealth.setBaseValue(100_000.0);
            mob.setHealth(mob.getMaxHealth());
            if (level.addFreshEntity(mob)) spawned++;
        }
        int result = spawned;
        context.getSource().sendSuccess(() -> Component.literal("Spawned " + result
                + " stationary RPG performance targets"), false);
        RpgProject.LOGGER.info("[RPG Skill Perf] spawned={} requested={} player={}",
                spawned, count, player.getScoreboardName());
        return spawned;
    }

    private static int clearPerformanceTargets(CommandContext<CommandSourceStack> context) {
        int removed = clearPerformanceTargets(context.getSource().getLevel());
        context.getSource().sendSuccess(() -> Component.literal("Removed " + removed
                + " RPG performance targets"), false);
        return removed;
    }

    private static int clearPerformanceTargets(net.minecraft.server.level.ServerLevel level) {
        int removed = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity.getTags().contains(PERFORMANCE_TARGET_TAG)) {
                entity.discard();
                removed++;
            }
        }
        return removed;
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        event.getEntity().getCapability(RpgCombatStateProvider.DATA).ifPresent(RpgCombatState::clear);
        if (event.getEntity() instanceof ServerPlayer deadPlayer) SkillRuntime.clearTransient(deadPlayer);
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) return;
        if (event.getEntity().getTags().contains(PERFORMANCE_TARGET_TAG)) return;
        long reward = Math.max(10L, Math.round(event.getEntity().getMaxHealth() * 5.0));
        player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
            int gained = data.addExperience(reward);
            int mobLevel = com.zexqm.rpgproject.world.MobLevelSystem.level(event.getEntity());
            long skillReward = SkillProgressionConfig.values().mobReward(mobLevel,
                    event.getEntity().getMaxHealth());
            int skillPointsGained = data.addSkillExperience(skillReward);
            syncRpg(player, data);
            CommonPacketSync.syncSkillProgress(player, data);
            if (gained > 0) player.displayClientMessage(Component.literal("Level Up! Level " + data.level()), false);
            if (skillPointsGained > 0) player.displayClientMessage(Component.literal(
                    "Skill Point +" + skillPointsGained), true);
            RpgProject.LOGGER.info("[RPG Progression] player={} target={} characterXp={} skillXp={} spGained={}",
                    player.getScoreboardName(), event.getEntity().getType().toShortString(), reward,
                    skillReward, skillPointsGained);
        });
    }
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(Mana.class);
        event.register(RpgPlayerData.class);
        event.register(RpgCombatState.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ManaProvider.ID, new ManaProvider());
            event.addCapability(RpgPlayerDataProvider.ID, new RpgPlayerDataProvider());
        }
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(RpgCombatStateProvider.ID, new RpgCombatStateProvider());
        }
    }

    @SubscribeEvent
    public static void tickLiving(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide) return;
        living.getCapability(RpgCombatStateProvider.DATA).ifPresent(state -> {
            boolean transition = state.tick(living) | RpgStatusService.tick(living);
            if (living instanceof ServerPlayer player) {
                int requested = state.consumePendingMpRecovery();
                if (requested > 0) {
                    Mana mana = player.getCapability(ManaProvider.MANA).orElse(null);
                    if (mana != null) {
                        int before = mana.getMana();
                        int recovered = mana.restore(requested);
                        if (recovered > 0) {
                            RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                    new SyncManaPacket(mana.getMana(), mana.getMaxMana()));
                            RpgProject.LOGGER.info("[RPG Skill] sustained-resource player={} requested={} recovered={} mana={}->{} remainingTicks={}",
                                    player.getScoreboardName(), requested, recovered, before, mana.getMana(),
                                    state.sustainedResourceTicks());
                        }
                    }
                }
            }
            if (transition || living.tickCount % 20 == 0) {
                RpgNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> living),
                        SyncEntityStatusesPacket.from(living.getId(), state.statuses().values()));
            }
            if (living instanceof ServerPlayer player && (transition || player.tickCount % 20 == 0)) {
                syncCombat(player, state);
            }
        });
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ManaProvider.MANA).ifPresent(oldMana ->
                event.getEntity().getCapability(ManaProvider.MANA).ifPresent(newMana -> newMana.copyFrom(oldMana)));
        event.getOriginal().getCapability(RpgPlayerDataProvider.DATA).ifPresent(oldData ->
                event.getEntity().getCapability(RpgPlayerDataProvider.DATA).ifPresent(newData -> newData.copyFrom(oldData)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> mana.setMaxMana((int) Math.round(data.stats().maxMana())));
            int before = mana.getMana();
            mana.tickRegen();
            if (before != mana.getMana() || player.tickCount % 40 == 0) {
                RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncManaPacket(mana.getMana(), mana.getMaxMana()));
            }
        });
        player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
            double loadRatio = WeightCalculator.loadRatio(player, data);
            if (data.exhausted() || loadRatio >= 1.0) player.setSprinting(false);
            data.tickStamina(player.isSprinting());
            double horizontalDistance = Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x
                    + player.getDeltaMovement().z * player.getDeltaMovement().z);
            data.trainFromMovement(horizontalDistance, loadRatio, player.isSprinting());
            applyAttributes(player, data, loadRatio);
            boolean changed = data.tick();
            boolean skillActive = SkillRuntime.tick(player);
            MovementPolicy movement = SkillRuntime.movementPolicy(player);
            if (movement == MovementPolicy.LOCKED || movement == MovementPolicy.ROTATE_ONLY) {
                Vec3 velocity = player.getDeltaMovement();
                player.setDeltaMovement(0.0, velocity.y, 0.0);
            } else if (movement == MovementPolicy.WALK && player.getDeltaMovement().horizontalDistanceSqr() > 0.04) {
                Vec3 velocity = player.getDeltaMovement();
                player.setDeltaMovement(velocity.x * 0.5, velocity.y, velocity.z * 0.5);
            }
            if (changed || skillActive || player.tickCount % 40 == 0) {
                syncRpg(player, data);
                RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncSkillStatePacket(data.actionState(), movement,
                                PrimaryResourceType.forClass(data.rpgClass()),
                                SkillRuntime.activeSkillId(player), data.actionTicks(),
                                SkillRuntime.activeCastTicks(player),
                                SkillRuntime.movementCancelAllowed(player),
                                SkillRuntime.aimMode(player)));
            }
        });
    }

    @SubscribeEvent
    public static void livingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide)
            player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> data.trainHealth(event.getAmount()));
    }

    private static void applyAttributes(ServerPlayer player, RpgPlayerData data, double loadRatio) {
        DerivedStats stats = data.stats();
        double trainingHealth = (data.healthTraining().level() - 1) * 2.0;
        setBase(player, Attributes.MAX_HEALTH, stats.maxHealth() + trainingHealth);
        RpgCombatState combatState = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        double sustainedMoveSpeed = combatState == null ? 0.0 : combatState.movementSpeedBonus();
        double timedMoveSpeed = combatState == null ? 0.0 : combatState.timedMovementSpeedBonus();
        double attackSpeed = combatState == null ? 0.0 : combatState.attackSpeedBonus();
        double castingSpeed = combatState == null ? 0.0 : combatState.castingSpeedBonus();
        double weightSpeed = loadRatio <= 0.70 ? 1.0
                : loadRatio <= 1.0 ? 1.0 - (loadRatio - 0.70) / 0.30 * 0.20
                : Math.max(0.35, 0.80 - (loadRatio - 1.0) / 0.25 * 0.45);
        setBase(player, Attributes.MOVEMENT_SPEED,
                0.1 * stats.moveSpeed() * weightSpeed * (1.0 + sustainedMoveSpeed + timedMoveSpeed));
        setBase(player, Attributes.ATTACK_SPEED, 4.0 * stats.attackSpeed());
        setTransientMultiplier(player, Attributes.ATTACK_SPEED, SPEED_SPELL_ATTACK_SPEED_ID,
                "RPG timed attack speed", attackSpeed);
        setBase(player, ModAttributes.ATTACK_POWER.get(), stats.attackPower());
        setBase(player, ModAttributes.MAGIC_POWER.get(), stats.magicPower());
        setBase(player, ModAttributes.DEFENSE.get(), stats.defense());
        setBase(player, ModAttributes.DAMAGE_REDUCTION.get(), stats.damageReduction());
        setBase(player, ModAttributes.ACCURACY.get(), stats.accuracy());
        setBase(player, ModAttributes.EVASION.get(), stats.evasion());
        setBase(player, ModAttributes.CRITICAL_CHANCE.get(), stats.criticalChance());
        setBase(player, ModAttributes.CRITICAL_DAMAGE.get(), stats.criticalDamage());
        setBase(player, ModAttributes.CAST_SPEED.get(), stats.castSpeed() * (1.0 + castingSpeed));
        setBase(player, ModAttributes.CC_RESISTANCE.get(), stats.ccResistance());
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setBase(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        var instance = player.getAttribute(attribute);
        if (instance != null && Math.abs(instance.getBaseValue() - value) > 0.001) {
            instance.setBaseValue(value);
        }
    }

    private static void setTransientMultiplier(ServerPlayer player,
                                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                               UUID id, String name, double amount) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        var current = instance.getModifier(id);
        if (current != null && Math.abs(current.getAmount() - amount) <= 0.001) return;
        if (current != null) instance.removeModifier(id);
        if (amount > 0) instance.addTransientModifier(new AttributeModifier(
                id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> syncRpg(player, data));
            player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> CommonPacketSync.syncSkillProgress(player, data));
            syncCombatState(player);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncSkillProgress(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillRuntime.clearTransient(player);
            com.zexqm.rpgproject.network.SkillProgressActionPacket.clearReplayState(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
            SkillRuntime.clearTransient(player);
            player.getCapability(RpgCombatStateProvider.DATA).ifPresent(RpgCombatState::clear);
            data.sheathe(); syncRpg(player, data);
            CommonPacketSync.syncSkillProgress(player, data);
        });
    }

    public static void syncRpg(ServerPlayer player, RpgPlayerData data) {
        RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncRpgDataPacket(
                data.weaponDrawn(), data.inCombat(), data.level(), data.experience(),
                data.requiredExperience(), data.availableSkillPoints(), data.stamina(), data.maxStamina(),
                data.breath().level(), data.strength().level(), data.healthTraining().level()));
    }

    private static void syncSkillProgress(ServerPlayer player) {
        player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data ->
                CommonPacketSync.syncSkillProgress(player, data));
    }

    private static int listSkills(CommandSourceStack source, int requestedPage)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
        if (data == null) return 0;
        java.util.List<com.zexqm.rpgproject.rpg.skill.SkillCatalogEntry> skills = SkillCatalog.all().stream()
                .filter(skill -> skill.rpgClass() == data.rpgClass())
                .sorted(java.util.Comparator.comparing(skill -> skill.id().toString())).toList();
        int pageSize = 8;
        int pages = Math.max(1, (skills.size() + pageSize - 1) / pageSize);
        int page = Math.min(requestedPage, pages);
        source.sendSuccess(() -> Component.literal("Skills " + data.rpgClass() + " page " + page + "/" + pages
                + " (use /rpg skills inspect <id> for details)"), false);
        skills.stream().skip((long) (page - 1) * pageSize).limit(pageSize).forEach(skill ->
                source.sendSuccess(() -> Component.literal(skill.id() + " rank="
                        + data.skillProgress().rank(skill.id()) + "/" + skill.maximumRank()
                        + " availability=" + SkillLearningService.availability(data, skill.id())), false));
        return Math.min(pageSize, Math.max(0, skills.size() - (page - 1) * pageSize));
    }

    private static int inspectSkill(CommandSourceStack source, net.minecraft.resources.ResourceLocation id)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RpgPlayerData data = player.getCapability(RpgPlayerDataProvider.DATA).orElse(null);
        var skill = SkillCatalog.get(id);
        if (data == null || skill == null) {
            source.sendFailure(Component.literal("Unknown catalog skill " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(skill.id() + " [" + skill.name() + "] MCP=" + skill.mcpId()
                + " tree=" + skill.tree() + " rank=" + data.skillProgress().rank(id) + "/"
                + skill.maximumRank() + " availability=" + SkillLearningService.availability(data, id)), false);
        source.sendSuccess(() -> Component.literal(skill.description().isBlank()
                ? "Description unavailable" : skill.description()), false);
        if (!skill.playable()) source.sendSuccess(() -> Component.literal("Gated: " + skill.unavailableReason()), false);
        String ranks = skill.ranks().stream().map(rank -> "R" + rank.rank() + " Lv" + rank.requiredLevel()
                        + " SP" + (rank.hasSkillPointCost() ? rank.skillPointCost() : "?"))
                .collect(java.util.stream.Collectors.joining(", "));
        if (!ranks.isBlank()) source.sendSuccess(() -> Component.literal(ranks), false);
        return 1;
    }

    private static int debugGroundCast(CommandContext<CommandSourceStack> context, double distance)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "skill");
        Vec3 origin = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 ground = origin.add(direction.scale(distance));
        RpgProject.LOGGER.info(
                "[RPG Skill] debug-cast-ground player={} skill={} distance={} origin={} ground={} direction={}",
                player.getScoreboardName(), id, distance, origin, ground, direction);
        var result = SkillRuntime.cast(player, id,
                new SkillExecutionContext(player, origin, direction, null, ground));
        context.getSource().sendSuccess(() -> Component.literal(
                "Ground cast distance=" + distance + " result=" + result), false);
        return result == com.zexqm.rpgproject.rpg.skill.SkillCastResult.STARTED ? 1 : 0;
    }

    private static int debugPerfectGuard(CommandContext<CommandSourceStack> context, int ticks)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RpgCombatState state = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (state == null) return 0;
        state.activateFrontGuard(ticks);
        state.activateSuperArmor(ticks);
        syncCombat(player, state);
        context.getSource().sendSuccess(() -> Component.literal(
                "Perfect Guard active for " + ticks + " ticks (FG + SA)"), false);
        return 1;
    }

    private static int debugImpact(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RpgCombatState state = player.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (state == null) return 0;

        String categoryValue = StringArgumentType.getString(context, "category").toLowerCase();
        CombatImpactCategory category = switch (categoryValue) {
            case "melee", "direct" -> CombatImpactCategory.DIRECT;
            case "projectile" -> CombatImpactCategory.PROJECTILE;
            case "explosion" -> CombatImpactCategory.EXPLOSION;
            default -> null;
        };
        if (category == null) {
            context.getSource().sendFailure(Component.literal("Category: melee, projectile, or explosion"));
            return 0;
        }

        Vec3 forward = player.getLookAngle().multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() <= 1.0E-8) forward = new Vec3(0, 0, 1);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        String direction = StringArgumentType.getString(context, "direction").toLowerCase();
        Vec3 offset = switch (direction) {
            case "front" -> forward;
            // Use 120 degrees so the probe is clearly outside the inclusive 180-degree FG boundary.
            case "side" -> right.scale(Math.sqrt(3.0) / 2.0).subtract(forward.scale(0.5));
            case "rear", "back" -> forward.scale(-1);
            default -> null;
        };
        if (offset == null) {
            context.getSource().sendFailure(Component.literal("Direction: front, side, or rear"));
            return 0;
        }

        float damage = IntegerArgumentType.getInteger(context, "damage");
        Vec3 origin = player.position().add(offset.scale(2.0));
        CombatImpactContext impact = CombatImpactContext.resolve(player, null, damage, category,
                origin, false, null, null, false);
        CombatImpactDiagnostics.Snapshot result = CombatImpactDiagnostics.probe(state, impact);
        CombatImpactDiagnostics.log("command-probe", player.getScoreboardName(), result);
        syncCombat(player, state);
        context.getSource().sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                "%s %s angle=%.1f protection=%s damage=%.1f->%.1f guard=%.1f->%.1f knockbackBlocked=%s",
                category, direction, result.facingAngleDegrees(), result.damageDecision().reason(),
                result.damageBefore(), result.damageAfter(), result.guardBefore(), result.guardAfter(),
                result.knockbackDecision().blockKnockback())), false);
        return 1;
    }

    public static void syncCombatState(ServerPlayer player) {
        player.getCapability(RpgCombatStateProvider.DATA).ifPresent(state -> syncCombat(player, state));
    }

    private static void syncCombat(ServerPlayer player, RpgCombatState state) {
        RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCombatStatePacket(
                state.guard(), state.maximumGuard(), state.ccPoints(), state.ccImmunityTicks(),
                state.activeCc(), state.activeCcTicks(), state.casting(), state.frontGuardTicks(),
                state.superArmorTicks(), state.iframeTicks(), state.grabImmuneTicks()));
    }

    private CommonEvents() {
    }
}
