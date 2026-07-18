package com.zexqm.rpgproject.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Synchronizes server-authoritative skill facing without rotating the detached camera. */
public record SyncPlayerFacingPacket(float yaw) {
    public static void encode(SyncPlayerFacingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.yaw);
    }

    public static SyncPlayerFacingPacket decode(FriendlyByteBuf buffer) {
        return new SyncPlayerFacingPacket(buffer.readFloat());
    }

    public static void handle(SyncPlayerFacingPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            player.setYRot(packet.yaw);
            player.setYHeadRot(packet.yaw);
            player.setYBodyRot(packet.yaw);
        });
        context.setPacketHandled(true);
    }
}
