package com.zexqm.rpgproject.network;

import com.zexqm.rpgproject.client.ClientEntityStatuses;
import com.zexqm.rpgproject.rpg.status.RpgStatusInstance;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record SyncEntityStatusesPacket(int entityId, List<Entry> statuses) {
    public static SyncEntityStatusesPacket from(int entityId, Iterable<RpgStatusInstance> statuses) {
        java.util.ArrayList<Entry> entries = new java.util.ArrayList<>();
        statuses.forEach(status -> entries.add(new Entry(status.type(), status.remainingTicks(),
                status.stacks(), status.potency())));
        return new SyncEntityStatusesPacket(entityId, List.copyOf(entries));
    }

    public static void encode(SyncEntityStatusesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeCollection(packet.statuses, (target, entry) -> {
            target.writeEnum(entry.type);
            target.writeVarInt(entry.remainingTicks);
            target.writeVarInt(entry.stacks);
            target.writeDouble(entry.potency);
        });
    }

    public static SyncEntityStatusesPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        List<Entry> entries = buffer.readList(source -> new Entry(source.readEnum(RpgStatusType.class),
                source.readVarInt(), source.readVarInt(), source.readDouble()));
        return new SyncEntityStatusesPacket(entityId, entries);
    }

    public static void handle(SyncEntityStatusesPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientEntityStatuses.apply(packet.entityId, packet.statuses));
        context.setPacketHandled(true);
    }

    public record Entry(RpgStatusType type, int remainingTicks, int stacks, double potency) {}
}
