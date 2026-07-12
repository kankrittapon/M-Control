package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.rpg.RpgPlayerDataProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.function.Supplier;

public final class ToggleCombatPacket {
    public static void encode(ToggleCombatPacket p, FriendlyByteBuf b) {}
    public static ToggleCombatPacket decode(FriendlyByteBuf b) { return new ToggleCombatPacket(); }
    public static void handle(ToggleCombatPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context c = supplier.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            if (player == null) return;
            player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                if (!data.toggleDraw()) player.displayClientMessage(Component.literal("Equip the correct Main and Sub weapons first."), true);
                RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncRpgDataPacket(
                        data.weaponDrawn(), data.inCombat(), data.level(), data.experience(),
                        data.requiredExperience(), data.availableSkillPoints(), data.stamina(), data.maxStamina(),
                        data.breath().level(), data.strength().level(), data.healthTraining().level()));
            });
        });
        c.setPacketHandled(true);
    }
}
