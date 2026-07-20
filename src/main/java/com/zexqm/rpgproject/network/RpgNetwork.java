package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RpgNetwork {
    private static final String PROTOCOL_VERSION = "5";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RpgProject.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncManaPacket.class, id++)
                .encoder(SyncManaPacket::encode)
                .decoder(SyncManaPacket::decode)
                .consumerMainThread(SyncManaPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncRpgDataPacket.class, id++)
                .encoder(SyncRpgDataPacket::encode).decoder(SyncRpgDataPacket::decode)
                .consumerMainThread(SyncRpgDataPacket::handle).add();
        CHANNEL.messageBuilder(ToggleCombatPacket.class, id++)
                .encoder(ToggleCombatPacket::encode).decoder(ToggleCombatPacket::decode)
                .consumerMainThread(ToggleCombatPacket::handle).add();
        CHANNEL.messageBuilder(SyncCombatStatePacket.class, id++)
                .encoder(SyncCombatStatePacket::encode).decoder(SyncCombatStatePacket::decode)
                .consumerMainThread(SyncCombatStatePacket::handle).add();
        CHANNEL.messageBuilder(CastSkillRequestPacket.class, id++)
                .encoder(CastSkillRequestPacket::encode).decoder(CastSkillRequestPacket::decode)
                .consumerMainThread(CastSkillRequestPacket::handle).add();
        CHANNEL.messageBuilder(SyncSkillStatePacket.class, id++)
                .encoder(SyncSkillStatePacket::encode).decoder(SyncSkillStatePacket::decode)
                .consumerMainThread(SyncSkillStatePacket::handle).add();
        CHANNEL.messageBuilder(SyncEntityStatusesPacket.class, id++)
                .encoder(SyncEntityStatusesPacket::encode).decoder(SyncEntityStatusesPacket::decode)
                .consumerMainThread(SyncEntityStatusesPacket::handle).add();
        CHANNEL.messageBuilder(SkillProgressActionPacket.class, id++)
                .encoder(SkillProgressActionPacket::encode).decoder(SkillProgressActionPacket::decode)
                .consumerMainThread(SkillProgressActionPacket::handle).add();
        CHANNEL.messageBuilder(SyncSkillProgressPacket.class, id)
                .encoder(SyncSkillProgressPacket::encode).decoder(SyncSkillProgressPacket::decode)
                .consumerMainThread(SyncSkillProgressPacket::handle).add();
        id++;
        CHANNEL.messageBuilder(CancelSkillActionPacket.class, id)
                .encoder(CancelSkillActionPacket::encode).decoder(CancelSkillActionPacket::decode)
                .consumerMainThread(CancelSkillActionPacket::handle).add();
        id++;
        CHANNEL.messageBuilder(SyncPlayerFacingPacket.class, id)
                .encoder(SyncPlayerFacingPacket::encode).decoder(SyncPlayerFacingPacket::decode)
                .consumerMainThread(SyncPlayerFacingPacket::handle).add();
        id++;
        CHANNEL.messageBuilder(RequestClientAimCastPacket.class, id)
                .encoder(RequestClientAimCastPacket::encode).decoder(RequestClientAimCastPacket::decode)
                .consumerMainThread(RequestClientAimCastPacket::handle).add();
        id++;
        CHANNEL.messageBuilder(BeginSkillTargetingPacket.class, id)
                .encoder(BeginSkillTargetingPacket::encode).decoder(BeginSkillTargetingPacket::decode)
                .consumerMainThread(BeginSkillTargetingPacket::handle).add();
        id++;
        CHANNEL.messageBuilder(SkillTargetAimPacket.class, id)
                .encoder(SkillTargetAimPacket::encode).decoder(SkillTargetAimPacket::decode)
                .consumerMainThread(SkillTargetAimPacket::handle).add();
    }

    private RpgNetwork() {
    }
}
