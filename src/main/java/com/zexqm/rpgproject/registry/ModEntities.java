package com.zexqm.rpgproject.registry;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.world.entity.MagicBoltProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RpgProject.MOD_ID);

    public static final RegistryObject<EntityType<MagicBoltProjectile>> MAGIC_BOLT = ENTITY_TYPES.register("magic_bolt",
            () -> EntityType.Builder.<MagicBoltProjectile>of(MagicBoltProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("magic_bolt"));

    private ModEntities() {
    }
}
