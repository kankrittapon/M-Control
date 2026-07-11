package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.mana.ManaProvider;
import com.zexqm.rpgproject.world.entity.MagicBoltProjectile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record CastMagicBoltPacket(Vec3 direction) {
    private static final int MANA_COST = 20;
    private static final double SPEED = 1.5D;

    public static void encode(CastMagicBoltPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.direction.x);
        buffer.writeDouble(packet.direction.y);
        buffer.writeDouble(packet.direction.z);
    }

    public static CastMagicBoltPacket decode(FriendlyByteBuf buffer) {
        return new CastMagicBoltPacket(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(CastMagicBoltPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                if (!mana.spend(MANA_COST)) {
                    player.displayClientMessage(Component.translatable("message.rpg_project.mana.not_enough"), true);
                    RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncManaPacket(mana.getMana(), mana.getMaxMana()));
                    return;
                }

                MagicBoltProjectile bolt = new MagicBoltProjectile(player.level(), player);
                bolt.setPos(player.getX(), player.getEyeY() - 0.15D, player.getZ());

                Vec3 direction = sanitizeDirection(packet.direction, player.getLookAngle());

                bolt.shoot(direction.x, direction.y, direction.z, (float) SPEED, 0.35F);
                player.level().addFreshEntity(bolt);
                RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncManaPacket(mana.getMana(), mana.getMaxMana()));
            });
        });
        context.get().setPacketHandled(true);
    }

    private static Vec3 sanitizeDirection(Vec3 requested, Vec3 fallback) {
        if (!Double.isFinite(requested.x) || !Double.isFinite(requested.y) || !Double.isFinite(requested.z) || requested.lengthSqr() < 1.0E-6D) {
            return fallback.normalize();
        }

        return requested.normalize();
    }
}
