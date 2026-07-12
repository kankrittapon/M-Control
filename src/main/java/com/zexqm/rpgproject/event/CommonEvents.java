package com.zexqm.rpgproject.event;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.Mana;
import com.zexqm.rpgproject.mana.ManaProvider;
import com.zexqm.rpgproject.network.RpgNetwork;
import com.zexqm.rpgproject.network.SyncManaPacket;
import com.zexqm.rpgproject.network.SyncRpgDataPacket;
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
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.zexqm.rpgproject.rpg.RpgClass;
import com.zexqm.rpgproject.rpg.Specialization;
import com.zexqm.rpgproject.rpg.DerivedStats;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.zexqm.rpgproject.registry.ModAttributes;
import com.zexqm.rpgproject.rpg.ClassProfileManager;
import com.zexqm.rpgproject.rpg.WeightCalculator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ClassProfileManager());
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
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> { data.setClass(value); syncRpg(player, data); });
                    return 1;
                })))
                .then(Commands.literal("specialization").then(Commands.argument("value", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Specialization value = Specialization.valueOf(StringArgumentType.getString(ctx, "value").toUpperCase());
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> { data.setSpecialization(value); syncRpg(player, data); });
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
                        player.displayClientMessage(Component.literal("EXP +" + amount + " Levels gained=" + gained), false);
                    });
                    return 1;
                })))
                .then(Commands.literal("setlevel").then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                        data.setLevel(IntegerArgumentType.getInteger(ctx, "level")); syncRpg(player, data);
                    });
                    return 1;
                })))
                .then(Commands.literal("protection")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String type = StringArgumentType.getString(ctx, "type").toLowerCase();
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                                        switch (type) {
                                            case "fg" -> data.protection().activateFrontGuard(ticks);
                                            case "sa" -> data.protection().activateSuperArmor(ticks);
                                            case "iframe" -> data.protection().activateIframe(ticks);
                                            case "grab_immune" -> data.protection().activateGrabImmunity(ticks);
                                            default -> player.displayClientMessage(Component.literal("Use fg, sa, iframe, or grab_immune"), false);
                                        }
                                    });
                                    return 1;
                                }))))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                        DerivedStats stats = data.stats();
                        player.displayClientMessage(Component.literal("Class=" + data.rpgClass() + " Spec=" + data.specialization()
                                + " Lv=" + data.level() + " EXP=" + data.experience() + "/" + data.requiredExperience()
                                + " SP=" + data.availableSkillPoints() + " Drawn=" + data.weaponDrawn() + " Ready=" + data.canDraw()), false);
                        player.displayClientMessage(Component.literal(String.format("HP=%.0f MP=%.0f AP=%.1f Magic=%.1f DEF=%.1f DR=%.1f%% ACC=%.1f EVA=%.1f",
                                stats.maxHealth(), stats.maxMana(), stats.attackPower(), stats.magicPower(), stats.defense(),
                                stats.damageReduction() * 100.0, stats.accuracy(), stats.evasion())), false);
                        player.displayClientMessage(Component.literal(String.format("Stamina=%.1f/%.1f Breath=%d Strength=%d Health=%d Weight=%.1f/%.1f",
                                data.stamina(), data.maxStamina(), data.breath().level(), data.strength().level(),
                                data.healthTraining().level(), WeightCalculator.carried(player), WeightCalculator.capacity(data))), false);
                        player.displayClientMessage(Component.literal(String.format("Guard=%.1f/%.1f FG=%s SA=%s Iframe=%s",
                                data.protection().guard(), data.protection().maxGuard(), data.protection().frontGuard(),
                                data.protection().superArmor(), data.protection().iframe())), false);
                    });
                    return 1;
                })));
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) return;
        long reward = Math.max(10L, Math.round(event.getEntity().getMaxHealth() * 5.0));
        player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
            int gained = data.addExperience(reward);
            syncRpg(player, data);
            if (gained > 0) player.displayClientMessage(Component.literal("Level Up! Level " + data.level()), false);
        });
    }
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(Mana.class);
        event.register(RpgPlayerData.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ManaProvider.ID, new ManaProvider());
            event.addCapability(RpgPlayerDataProvider.ID, new RpgPlayerDataProvider());
        }
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
            if (changed || player.tickCount % 40 == 0) syncRpg(player, data);
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
        double weightSpeed = loadRatio <= 0.70 ? 1.0
                : loadRatio <= 1.0 ? 1.0 - (loadRatio - 0.70) / 0.30 * 0.20
                : Math.max(0.35, 0.80 - (loadRatio - 1.0) / 0.25 * 0.45);
        setBase(player, Attributes.MOVEMENT_SPEED, 0.1 * stats.moveSpeed() * weightSpeed);
        setBase(player, ModAttributes.ATTACK_POWER.get(), stats.attackPower());
        setBase(player, ModAttributes.MAGIC_POWER.get(), stats.magicPower());
        setBase(player, ModAttributes.DEFENSE.get(), stats.defense());
        setBase(player, ModAttributes.DAMAGE_REDUCTION.get(), stats.damageReduction());
        setBase(player, ModAttributes.ACCURACY.get(), stats.accuracy());
        setBase(player, ModAttributes.EVASION.get(), stats.evasion());
        setBase(player, ModAttributes.CRITICAL_CHANCE.get(), stats.criticalChance());
        setBase(player, ModAttributes.CRITICAL_DAMAGE.get(), stats.criticalDamage());
        setBase(player, ModAttributes.CAST_SPEED.get(), stats.castSpeed());
        setBase(player, ModAttributes.CC_RESISTANCE.get(), stats.ccResistance());
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setBase(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        var instance = player.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value) instance.setBaseValue(value);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> syncRpg(player, data));
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
            data.sheathe(); syncRpg(player, data);
        });
    }

    public static void syncRpg(ServerPlayer player, RpgPlayerData data) {
        RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncRpgDataPacket(
                data.weaponDrawn(), data.inCombat(), data.level(), data.experience(),
                data.requiredExperience(), data.availableSkillPoints(), data.stamina(), data.maxStamina(),
                data.breath().level(), data.strength().level(), data.healthTraining().level()));
    }

    private CommonEvents() {
    }
}
