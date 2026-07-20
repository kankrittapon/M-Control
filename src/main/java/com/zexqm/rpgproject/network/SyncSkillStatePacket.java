package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.client.ClientSkillState;
import com.zexqm.rpgproject.rpg.skill.MovementPolicy;
import com.zexqm.rpgproject.rpg.skill.PrimaryResourceType;
import com.zexqm.rpgproject.rpg.skill.SkillActionState;
import com.zexqm.rpgproject.rpg.skill.SkillAimMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSkillStatePacket(SkillActionState action, MovementPolicy movement,
                                   PrimaryResourceType resource, ResourceLocation activeSkill,
                                   int actionTicks, int castTicks, boolean movementCancelAllowed,
                                   SkillAimMode aimMode) {
    public static void encode(SyncSkillStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeEnum(packet.movement);
        buffer.writeEnum(packet.resource);
        buffer.writeBoolean(packet.activeSkill != null);
        if (packet.activeSkill != null) buffer.writeResourceLocation(packet.activeSkill);
        buffer.writeVarInt(packet.actionTicks);
        buffer.writeVarInt(packet.castTicks);
        buffer.writeBoolean(packet.movementCancelAllowed);
        buffer.writeEnum(packet.aimMode);
    }

    public static SyncSkillStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncSkillStatePacket(buffer.readEnum(SkillActionState.class),
                buffer.readEnum(MovementPolicy.class), buffer.readEnum(PrimaryResourceType.class),
                buffer.readBoolean() ? buffer.readResourceLocation() : null,
                buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(),
                buffer.readEnum(SkillAimMode.class));
    }

    public static void handle(SyncSkillStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientSkillState.apply(packet.action, packet.movement, packet.resource,
                packet.activeSkill, packet.actionTicks, packet.castTicks,
                packet.movementCancelAllowed, packet.aimMode));

        context.setPacketHandled(true);
    }
}
