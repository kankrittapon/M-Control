package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.client.ClientAim;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests a development cast using the local detached camera's actual aim ray. */
public record RequestClientAimCastPacket(ResourceLocation skillId) {
    public static void encode(RequestClientAimCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.skillId);
    }

    public static RequestClientAimCastPacket decode(FriendlyByteBuf buffer) {
        return new RequestClientAimCastPacket(buffer.readResourceLocation());
    }

    public static void handle(RequestClientAimCastPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) return;
            ClientAim.AimResult aim = ClientAim.current(minecraft);
            Integer targetId = aim.entityHit() == null ? null : aim.entityHit().getEntity().getId();
            var ground = aim.blockHit() != null && aim.blockHit().getType() != HitResult.Type.MISS
                    ? aim.blockHit().getLocation() : aim.targetPoint();
            int lateralSide = minecraft.options.keyLeft.isDown() ? -1
                    : minecraft.options.keyRight.isDown() ? 1 : 0;
            int forwardAxis = minecraft.options.keyUp.isDown() ? 1
                    : minecraft.options.keyDown.isDown() ? -1 : 0;
            RpgNetwork.CHANNEL.sendToServer(new CastSkillRequestPacket(
                    packet.skillId, aim.direction(), targetId, ground, lateralSide, forwardAxis));
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Debug client aim: target="
                            + (targetId == null ? "none" : targetId) + " ground=" + ground), false);
        });
        context.setPacketHandled(true);
    }
}
