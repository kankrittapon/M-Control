package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.client.ClientRpgData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncRpgDataPacket(boolean weaponDrawn, boolean inCombat, int level, long experience,
                                long requiredExperience, int skillPoints, double stamina, double maxStamina,
                                int breathLevel, int strengthLevel, int healthLevel) {
    public static void encode(SyncRpgDataPacket p, FriendlyByteBuf b) {
        b.writeBoolean(p.weaponDrawn); b.writeBoolean(p.inCombat); b.writeVarInt(p.level);
        b.writeVarLong(p.experience); b.writeVarLong(p.requiredExperience); b.writeVarInt(p.skillPoints);
        b.writeDouble(p.stamina); b.writeDouble(p.maxStamina); b.writeVarInt(p.breathLevel);
        b.writeVarInt(p.strengthLevel); b.writeVarInt(p.healthLevel);
    }
    public static SyncRpgDataPacket decode(FriendlyByteBuf b) {
        return new SyncRpgDataPacket(b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readVarLong(),
                b.readVarLong(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }
    public static void handle(SyncRpgDataPacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> ClientRpgData.set(p.weaponDrawn, p.inCombat, p.level, p.experience,
                p.requiredExperience, p.skillPoints, p.stamina, p.maxStamina,
                p.breathLevel, p.strengthLevel, p.healthLevel));
        c.get().setPacketHandled(true);
    }
}
