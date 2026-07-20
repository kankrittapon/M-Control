package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public final class TauntBeaconTargetEvent extends Event {
    private final ServerPlayer owner;
    private final ArmorStand beacon;
    private final Mob mob;

    public TauntBeaconTargetEvent(ServerPlayer owner, ArmorStand beacon, Mob mob) {
        this.owner = owner;
        this.beacon = beacon;
        this.mob = mob;
    }

    public ServerPlayer owner() { return owner; }
    public ArmorStand beacon() { return beacon; }
    public Mob mob() { return mob; }
}
