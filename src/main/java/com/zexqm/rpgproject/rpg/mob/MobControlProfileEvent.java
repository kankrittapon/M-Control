package com.zexqm.rpgproject.rpg.mob;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

public final class MobControlProfileEvent extends Event {
    private final LivingEntity entity;
    private MobControlProfile profile;

    public MobControlProfileEvent(LivingEntity entity, MobControlProfile profile) {
        this.entity = entity;
        this.profile = profile;
    }

    public LivingEntity entity() { return entity; }
    public MobControlProfile profile() { return profile; }
    public void setProfile(MobControlProfile profile) { this.profile = profile; }
}
