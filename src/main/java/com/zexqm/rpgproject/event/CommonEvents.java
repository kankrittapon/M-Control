package com.zexqm.rpgproject.event;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.Mana;
import com.zexqm.rpgproject.mana.ManaProvider;
import com.zexqm.rpgproject.network.RpgNetwork;
import com.zexqm.rpgproject.network.SyncManaPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(Mana.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ManaProvider.ID, new ManaProvider());
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ManaProvider.MANA).ifPresent(oldMana ->
                event.getEntity().getCapability(ManaProvider.MANA).ifPresent(newMana -> newMana.copyFrom(oldMana)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            int before = mana.getMana();
            mana.tickRegen();
            if (before != mana.getMana() || player.tickCount % 40 == 0) {
                RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncManaPacket(mana.getMana(), mana.getMaxMana()));
            }
        });
    }

    private CommonEvents() {
    }
}
