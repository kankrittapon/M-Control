package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.rpg.skill.SkillCancelTrigger;
import com.zexqm.rpgproject.rpg.skill.SkillRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CancelSkillActionPacket(SkillCancelTrigger trigger) {
    public static void encode(CancelSkillActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.trigger);
    }

    public static CancelSkillActionPacket decode(FriendlyByteBuf buffer) {
        return new CancelSkillActionPacket(buffer.readEnum(SkillCancelTrigger.class));
    }

    public static void handle(CancelSkillActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null && packet.trigger == SkillCancelTrigger.MOVEMENT)
                SkillRuntime.requestCancel(context.getSender(), packet.trigger);
        });
        context.setPacketHandled(true);
    }
}
