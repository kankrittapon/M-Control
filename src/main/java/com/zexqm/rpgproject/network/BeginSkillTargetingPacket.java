package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.rpg.skill.SkillRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BeginSkillTargetingPacket(ResourceLocation skillId) {
    public static void encode(BeginSkillTargetingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.skillId);
    }

    public static BeginSkillTargetingPacket decode(FriendlyByteBuf buffer) {
        return new BeginSkillTargetingPacket(buffer.readResourceLocation());
    }

    public static void handle(BeginSkillTargetingPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) SkillRuntime.beginTargeting(player, packet.skillId);
        });
        context.setPacketHandled(true);
    }
}
