package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.rpg.skill.SkillExecutionContext;
import com.zexqm.rpgproject.rpg.skill.SkillRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CastSkillRequestPacket(ResourceLocation skillId, Vec3 direction,
                                     Integer targetEntityId, Vec3 groundPosition, int lateralSide,
                                     int forwardAxis) {
    private static final double MAX_COORDINATE = 3.0E7;

    public static void encode(CastSkillRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.skillId);
        buffer.writeDouble(packet.direction.x);
        buffer.writeDouble(packet.direction.y);
        buffer.writeDouble(packet.direction.z);
        buffer.writeByte(packet.lateralSide);
        buffer.writeByte(packet.forwardAxis);
        buffer.writeBoolean(packet.targetEntityId != null);
        if (packet.targetEntityId != null) buffer.writeVarInt(packet.targetEntityId);
        buffer.writeBoolean(packet.groundPosition != null);
        if (packet.groundPosition != null) {
            buffer.writeDouble(packet.groundPosition.x);
            buffer.writeDouble(packet.groundPosition.y);
            buffer.writeDouble(packet.groundPosition.z);
        }
    }

    public static CastSkillRequestPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        Vec3 direction = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        int lateralSide = buffer.readByte();
        int forwardAxis = buffer.readByte();
        Integer target = buffer.readBoolean() ? buffer.readVarInt() : null;
        Vec3 ground = buffer.readBoolean()
                ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()) : null;
        return new CastSkillRequestPacket(id, direction, target, ground, lateralSide, forwardAxis);
    }

    public static void handle(CastSkillRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.lateralSide < -1 || packet.lateralSide > 1
                    || packet.forwardAxis < -1 || packet.forwardAxis > 1
                    || !finite(packet.direction) || packet.direction.lengthSqr() < 0.25
                    || packet.direction.lengthSqr() > 2.25
                    || packet.groundPosition != null && (!finite(packet.groundPosition)
                    || Math.abs(packet.groundPosition.x) > MAX_COORDINATE
                    || Math.abs(packet.groundPosition.z) > MAX_COORDINATE)) return;
            SkillRuntime.cast(player, packet.skillId, new SkillExecutionContext(player, player.getEyePosition(),
                    packet.direction.normalize(), packet.targetEntityId, packet.groundPosition,
                    packet.lateralSide, packet.forwardAxis));
        });
        context.setPacketHandled(true);
    }

    public CastSkillRequestPacket(ResourceLocation skillId, Vec3 direction,
                                  Integer targetEntityId, Vec3 groundPosition) {
        this(skillId, direction, targetEntityId, groundPosition, 0, 0);
    }

    public CastSkillRequestPacket(ResourceLocation skillId, Vec3 direction,
                                  Integer targetEntityId, Vec3 groundPosition, int lateralSide) {
        this(skillId, direction, targetEntityId, groundPosition, lateralSide, 0);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
