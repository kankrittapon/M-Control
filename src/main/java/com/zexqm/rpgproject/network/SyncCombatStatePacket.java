package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.client.ClientCombatState;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCombatStatePacket(double guard, double maximumGuard, double ccPoints,
                                    int immunityTicks, CrowdControlType activeCc,
                                    int activeTicks, boolean casting, int frontGuardTicks,
                                    int superArmorTicks, int iframeTicks, int grabImmuneTicks) {
    public static void encode(SyncCombatStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.guard);
        buffer.writeDouble(packet.maximumGuard);
        buffer.writeDouble(packet.ccPoints);
        buffer.writeVarInt(packet.immunityTicks);
        buffer.writeBoolean(packet.activeCc != null);
        if (packet.activeCc != null) buffer.writeEnum(packet.activeCc);
        buffer.writeVarInt(packet.activeTicks);
        buffer.writeBoolean(packet.casting);
        buffer.writeVarInt(packet.frontGuardTicks);
        buffer.writeVarInt(packet.superArmorTicks);
        buffer.writeVarInt(packet.iframeTicks);
        buffer.writeVarInt(packet.grabImmuneTicks);
    }

    public static SyncCombatStatePacket decode(FriendlyByteBuf buffer) {
        double guard = buffer.readDouble();
        double maximumGuard = buffer.readDouble();
        double ccPoints = buffer.readDouble();
        int immunityTicks = buffer.readVarInt();
        CrowdControlType activeCc = buffer.readBoolean() ? buffer.readEnum(CrowdControlType.class) : null;
        return new SyncCombatStatePacket(guard, maximumGuard, ccPoints, immunityTicks,
                activeCc, buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncCombatStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientCombatState.apply(packet));
        context.setPacketHandled(true);
    }
}
