package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.rpg.skill.SkillAvailability;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SkillProgressPacketTest {
    private static final ResourceLocation SKILL = new ResourceLocation("rpg_project", "packet_test");

    @Test
    void syncPacketRoundTripsRanksAvailabilityAndPoints() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SyncSkillProgressPacket original = new SyncSkillProgressPacket(Map.of(SKILL, 2),
                    Map.of(SKILL, SkillAvailability.MAX_RANK), 17);
            SyncSkillProgressPacket.encode(original, buffer);
            assertEquals(original, SyncSkillProgressPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void syncPacketRejectsOversizedMaps() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(1025);
            assertThrows(IllegalArgumentException.class, () -> SyncSkillProgressPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void progressionNonceRejectsReplayAndClearsOnLogout() {
        UUID player = UUID.randomUUID();
        SkillProgressActionPacket.clearReplayState(player);
        assertTrue(SkillProgressActionPacket.acceptNonce(player, 4));
        assertFalse(SkillProgressActionPacket.acceptNonce(player, 4));
        assertFalse(SkillProgressActionPacket.acceptNonce(player, 3));
        assertFalse(SkillProgressActionPacket.acceptNonce(player, -1));
        assertTrue(SkillProgressActionPacket.acceptNonce(player, 5));
        SkillProgressActionPacket.clearReplayState(player);
        assertTrue(SkillProgressActionPacket.acceptNonce(player, 0));
        SkillProgressActionPacket.clearReplayState(player);
    }
}
