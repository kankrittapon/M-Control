package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.rpg.RpgPlayerData;
import com.zexqm.rpgproject.rpg.skill.SkillCatalog;
import com.zexqm.rpgproject.rpg.skill.SkillLearningService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class CommonPacketSync {
    public static void syncSkillProgress(ServerPlayer player, RpgPlayerData data) {
        java.util.Map<net.minecraft.resources.ResourceLocation,
                com.zexqm.rpgproject.rpg.skill.SkillAvailability> availability = new java.util.HashMap<>();
        SkillCatalog.all().forEach(skill -> availability.put(skill.id(),
                SkillLearningService.availability(data, skill.id())));
        RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncSkillProgressPacket(data.skillProgress().learnedRanks(), availability,
                        data.availableSkillPoints()));
    }

    private CommonPacketSync() {}
}
