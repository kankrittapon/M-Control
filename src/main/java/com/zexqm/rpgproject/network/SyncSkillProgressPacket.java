package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.client.ClientSkillProgress;
import com.zexqm.rpgproject.rpg.skill.SkillAvailability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncSkillProgressPacket(Map<ResourceLocation, Integer> learnedRanks,
                                      Map<ResourceLocation, SkillAvailability> availability,
                                      int totalSkillPoints, int spentSkillPoints,
                                      long skillExperience, long requiredSkillExperience) {
    public static void encode(SyncSkillProgressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.learnedRanks.size());
        packet.learnedRanks.forEach((id, rank) -> {
            buffer.writeResourceLocation(id);
            buffer.writeVarInt(rank);
        });
        buffer.writeVarInt(packet.availability.size());
        packet.availability.forEach((id, value) -> {
            buffer.writeResourceLocation(id);
            buffer.writeEnum(value);
        });
        buffer.writeVarInt(packet.totalSkillPoints);
        buffer.writeVarInt(packet.spentSkillPoints);
        buffer.writeVarLong(packet.skillExperience);
        buffer.writeVarLong(packet.requiredSkillExperience);
    }

    public static SyncSkillProgressPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 1024) throw new IllegalArgumentException("Invalid learned skill count " + size);
        Map<ResourceLocation, Integer> ranks = new HashMap<>();
        for (int index = 0; index < size; index++) ranks.put(buffer.readResourceLocation(), buffer.readVarInt());
        int availabilitySize = buffer.readVarInt();
        if (availabilitySize < 0 || availabilitySize > 1024)
            throw new IllegalArgumentException("Invalid skill availability count " + availabilitySize);
        Map<ResourceLocation, SkillAvailability> availability = new HashMap<>();
        for (int index = 0; index < availabilitySize; index++)
            availability.put(buffer.readResourceLocation(), buffer.readEnum(SkillAvailability.class));
        return new SyncSkillProgressPacket(Map.copyOf(ranks), Map.copyOf(availability),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarLong(), buffer.readVarLong());
    }

    public static void handle(SyncSkillProgressPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientSkillProgress.apply(packet.learnedRanks, packet.availability,
                packet.totalSkillPoints, packet.spentSkillPoints, packet.skillExperience,
                packet.requiredSkillExperience));
        context.setPacketHandled(true);
    }
}
