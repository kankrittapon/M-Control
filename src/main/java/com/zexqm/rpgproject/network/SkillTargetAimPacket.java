package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.rpg.skill.SkillExecutionContext;
import com.zexqm.rpgproject.rpg.skill.SkillRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SkillTargetAimPacket(ResourceLocation skillId, boolean confirm, Vec3 direction,
                                   Integer targetEntityId, Vec3 groundPosition) {
    private static final double MAX_COORDINATE = 3.0E7;

    public static void encode(SkillTargetAimPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.skillId);
        buffer.writeBoolean(packet.confirm);
        buffer.writeDouble(packet.direction.x);
        buffer.writeDouble(packet.direction.y);
        buffer.writeDouble(packet.direction.z);
        buffer.writeBoolean(packet.targetEntityId != null);
        if (packet.targetEntityId != null) buffer.writeVarInt(packet.targetEntityId);
        buffer.writeBoolean(packet.groundPosition != null);
        if (packet.groundPosition != null) {
            buffer.writeDouble(packet.groundPosition.x);
            buffer.writeDouble(packet.groundPosition.y);
            buffer.writeDouble(packet.groundPosition.z);
        }
    }

    public static SkillTargetAimPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation skill = buffer.readResourceLocation();
        boolean confirm = buffer.readBoolean();
        Vec3 direction = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Integer target = buffer.readBoolean() ? buffer.readVarInt() : null;
        Vec3 ground = buffer.readBoolean()
                ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()) : null;
        return new SkillTargetAimPacket(skill, confirm, direction, target, ground);
    }

    public static void handle(SkillTargetAimPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context network = supplier.get();
        network.enqueueWork(() -> {
            var player = network.getSender();
            if (player == null || !finite(packet.direction)
                    || packet.direction.lengthSqr() < 0.25 || packet.direction.lengthSqr() > 2.25
                    || packet.groundPosition != null && (!finite(packet.groundPosition)
                    || Math.abs(packet.groundPosition.x) > MAX_COORDINATE
                    || Math.abs(packet.groundPosition.z) > MAX_COORDINATE)) return;
            SkillExecutionContext context = new SkillExecutionContext(player, player.getEyePosition(),
                    packet.direction.normalize(), packet.targetEntityId, packet.groundPosition);
            if (packet.confirm) SkillRuntime.confirmTargeting(player, packet.skillId, context);
            else SkillRuntime.updateChannelTarget(player, packet.skillId, context);
        });
        network.setPacketHandled(true);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
