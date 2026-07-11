package com.zexqm.rpgproject.world.entity;

import com.zexqm.rpgproject.registry.ModEntities;
import com.zexqm.rpgproject.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class MagicBoltProjectile extends ThrowableItemProjectile {
    private static final float DAMAGE = 6.0F;

    public MagicBoltProjectile(EntityType<? extends MagicBoltProjectile> type, Level level) {
        super(type, level);
    }

    public MagicBoltProjectile(Level level, LivingEntity owner) {
        super(ModEntities.MAGIC_BOLT.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MANA_CRYSTAL.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity target && getOwner() instanceof LivingEntity owner) {
            target.hurt(damageSources().magic(), DAMAGE);
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }
}
