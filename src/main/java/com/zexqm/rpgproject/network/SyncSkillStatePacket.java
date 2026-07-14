package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.client.ClientSkillState;
import com.zexqm.rpgproject.rpg.skill.MovementPolicy;
import com.zexqm.rpgproject.rpg.skill.PrimaryResourceType;
import com.zexqm.rpgproject.rpg.skill.SkillActionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSkillStatePacket(SkillActionState action, MovementPolicy movement,
                                   PrimaryResourceType resource) {
    public static void encode(SyncSkillStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeEnum(packet.movement);
        buffer.writeEnum(packet.resource);
    }

    public static SyncSkillStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncSkillStatePacket(buffer.readEnum(SkillActionState.class),
                buffer.readEnum(MovementPolicy.class), buffer.readEnum(PrimaryResourceType.class));
    }

    public static void handle(SyncSkillStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientSkillState.apply(packet.action, packet.movement, packet.resource));
        context.setPacketHandled(true);
    }
}
