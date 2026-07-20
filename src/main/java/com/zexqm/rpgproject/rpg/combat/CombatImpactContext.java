package com.zexqm.rpgproject.rpg.combat;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import com.zexqm.rpgproject.api.combat.CombatImpactResolveEvent;

public record CombatImpactContext(LivingEntity target, Entity directEntity, Entity attacker,
                                  Vec3 origin, DamageSource source, CombatImpactCategory category,
                                  float incomingDamage, boolean bypassesInvulnerability,
                                  boolean rpgTagged) {
    public static CombatImpactContext fromDamage(LivingEntity target, DamageSource source, float damage,
                                                  boolean rpgTagged) {
        CombatImpactCategory category;
        if (rpgTagged) category = CombatImpactCategory.RPG;
        else if (source.is(DamageTypeTags.IS_EXPLOSION)) category = CombatImpactCategory.EXPLOSION;
        else if (source.is(DamageTypeTags.IS_PROJECTILE)) category = CombatImpactCategory.PROJECTILE;
        else if (source.getEntity() != null) category = CombatImpactCategory.DIRECT;
        else category = CombatImpactCategory.ENVIRONMENT;
        return resolve(target, source, damage, category, source.getSourcePosition(), rpgTagged,
                source.getDirectEntity(), source.getEntity(),
                source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));
    }

    public static CombatImpactContext fromKnockback(LivingEntity target, double ratioX, double ratioZ,
                                                     float strength) {
        Vec3 horizontal = new Vec3(ratioX, 0, ratioZ);
        Vec3 origin = horizontal.lengthSqr() > 1.0E-8 ? target.position().add(horizontal) : null;
        return resolve(target, null, strength, CombatImpactCategory.UNKNOWN, origin,
                false, null, null, false);
    }

    public static CombatImpactContext resolve(LivingEntity target, DamageSource source, float damage,
                                              CombatImpactCategory category, Vec3 origin, boolean rpgTagged,
                                              Entity directEntity, Entity attacker,
                                              boolean bypassesInvulnerability) {
        CombatImpactResolveEvent event = new CombatImpactResolveEvent(target, source, damage,
                category, origin, rpgTagged);
        MinecraftForge.EVENT_BUS.post(event);
        return new CombatImpactContext(target, directEntity, attacker, event.origin(), source,
                event.category(), damage, bypassesInvulnerability, event.rpgTagged());
    }

    public boolean hasDirectionalOrigin() {
        return origin != null && origin.distanceToSqr(target.position()) > 1.0E-8;
    }
}
