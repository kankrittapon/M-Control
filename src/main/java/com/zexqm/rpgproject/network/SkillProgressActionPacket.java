package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.rpg.RpgPlayerDataProvider;
import com.zexqm.rpgproject.rpg.skill.SkillLearningService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.UUID;

public record SkillProgressActionPacket(ResourceLocation skillId, Operation operation, long nonce) {
    private static final java.util.Map<java.util.UUID, Long> LAST_NONCE = new java.util.HashMap<>();

    public static void encode(SkillProgressActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.skillId);
        buffer.writeEnum(packet.operation);
        buffer.writeVarLong(packet.nonce);
    }

    public static SkillProgressActionPacket decode(FriendlyByteBuf buffer) {
        return new SkillProgressActionPacket(buffer.readResourceLocation(), buffer.readEnum(Operation.class),
                buffer.readVarLong());
    }

    public static void handle(SkillProgressActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!acceptNonce(player.getUUID(), packet.nonce)) {
                com.zexqm.rpgproject.RpgProject.LOGGER.debug(
                        "[RPG Skill Learn] player={} skill={} result=REJECTED reason=REPLAYED_NONCE nonce={}",
                        player.getScoreboardName(), packet.skillId, packet.nonce);
                return;
            }
            player.getCapability(RpgPlayerDataProvider.DATA).ifPresent(data -> {
                var result = packet.operation == Operation.UPGRADE
                        ? SkillLearningService.upgrade(data, packet.skillId)
                        : SkillLearningService.downgrade(data, packet.skillId);
                CommonPacketSync.syncSkillProgress(player, data);
                com.zexqm.rpgproject.RpgProject.LOGGER.info(
                        "[RPG Skill Learn] player={} operation={} skill={} result={}",
                        player.getScoreboardName(), packet.operation, packet.skillId, result);
            });
        });
        context.setPacketHandled(true);
    }

    public static void clearReplayState(ServerPlayer player) {
        LAST_NONCE.remove(player.getUUID());
    }

    static boolean acceptNonce(UUID playerId, long nonce) {
        if (nonce < 0 || nonce <= LAST_NONCE.getOrDefault(playerId, -1L)) return false;
        LAST_NONCE.put(playerId, nonce);
        return true;
    }

    static void clearReplayState(UUID playerId) { LAST_NONCE.remove(playerId); }

    public enum Operation { UPGRADE, DOWNGRADE }
}
