package com.zexqm.rpgproject.rpg.mob;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

public final class MobControlProfiles {
    public static final TagKey<EntityType<?>> ELITE = tag("control/elite");
    public static final TagKey<EntityType<?>> BOSS = tag("control/boss");
    public static final TagKey<EntityType<?>> UNSTOPPABLE = tag("control/unstoppable");

    public static MobControlProfile resolve(LivingEntity entity) {
        if (entity instanceof Player) return MobControlProfile.PLAYER;
        MobControlProfile profile = entity.getType().is(UNSTOPPABLE) ? MobControlProfile.UNSTOPPABLE
                : entity.getType().is(BOSS) ? MobControlProfile.BOSS
                : entity.getType().is(ELITE) ? MobControlProfile.ELITE
                : MobControlProfile.NORMAL;
        MobControlProfileEvent event = new MobControlProfileEvent(entity, profile);
        MinecraftForge.EVENT_BUS.post(event);
        return event.profile();
    }

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(RpgProject.MOD_ID, path));
    }

    private MobControlProfiles() {}
}
