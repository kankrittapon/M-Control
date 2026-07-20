package com.zexqm.rpgproject.api.combat;

import com.zexqm.rpgproject.rpg.combat.CombatImpactCategory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

public final class CombatImpactResolveEvent extends Event {
    private final LivingEntity target;
    private final @Nullable DamageSource source;
    private final float incomingDamage;
    private CombatImpactCategory category;
    private @Nullable Vec3 origin;
    private boolean rpgTagged;

    public CombatImpactResolveEvent(LivingEntity target, @Nullable DamageSource source, float incomingDamage,
                                    CombatImpactCategory category, @Nullable Vec3 origin, boolean rpgTagged) {
        this.target = target;
        this.source = source;
        this.incomingDamage = incomingDamage;
        this.category = category;
        this.origin = origin;
        this.rpgTagged = rpgTagged;
    }

    public LivingEntity target() { return target; }
    public @Nullable DamageSource source() { return source; }
    public float incomingDamage() { return incomingDamage; }
    public CombatImpactCategory category() { return category; }
    public @Nullable Vec3 origin() { return origin; }
    public boolean rpgTagged() { return rpgTagged; }

    public void setCategory(CombatImpactCategory category) {
        if (category == null) throw new IllegalArgumentException("Impact category is required");
        this.category = category;
    }

    public void setOrigin(@Nullable Vec3 origin) {
        if (origin != null && (!Double.isFinite(origin.x) || !Double.isFinite(origin.y)
                || !Double.isFinite(origin.z))) throw new IllegalArgumentException("Impact origin must be finite");
        this.origin = origin;
    }

    public void setRpgTagged(boolean rpgTagged) { this.rpgTagged = rpgTagged; }
}
