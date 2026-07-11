package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.mana.ClientMana;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncManaPacket(int mana, int maxMana) {
    public static void encode(SyncManaPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.mana);
        buffer.writeInt(packet.maxMana);
    }

    public static SyncManaPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaPacket(buffer.readInt(), buffer.readInt());
    }

    public static void handle(SyncManaPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ClientMana.set(packet.mana, packet.maxMana));
        context.get().setPacketHandled(true);
    }
}
