package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RpgNetwork {
    private static final String PROTOCOL_VERSION = "1";

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
        CHANNEL.messageBuilder(CastMagicBoltPacket.class, id)
                .encoder(CastMagicBoltPacket::encode)
                .decoder(CastMagicBoltPacket::decode)
                .consumerMainThread(CastMagicBoltPacket::handle)
                .add();
    }

    private RpgNetwork() {
    }
}
