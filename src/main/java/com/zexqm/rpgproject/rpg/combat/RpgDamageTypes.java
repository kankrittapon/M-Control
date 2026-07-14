package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public final class RpgDamageTypes {
    public static final ResourceKey<DamageType> RPG = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(RpgProject.MOD_ID, "rpg"));

    public static DamageSource source(LivingEntity attacker) {
        var holder = attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(RPG);
        return new DamageSource(holder, attacker);
    }

    private RpgDamageTypes() {}
}
