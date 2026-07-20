package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.rpg.CrowdControlType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import com.zexqm.rpgproject.rpg.status.RpgStatusInstance;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;

import java.util.EnumMap;
import java.util.Map;

public final class RpgCombatState {
    private int frontGuardTicks;
    private int superArmorTicks;
    private int iframeTicks;
    private int grabImmuneTicks;
    private double guard = 100.0;
    private double maximumGuard = 100.0;
    private double ccPoints;
    private int ccImmunityTicks;
    private int ccChainResetTicks;
    private CrowdControlType activeCc;
    private int activeCcTicks;
    private boolean casting;
    private int airSmashLandingTicks;
    private int manaShieldTicks;
    private double manaShieldRatio;
    private int resistanceBuffTicks;
    private double resistanceBuff;
    private int damageReductionBuffTicks;
    private double damageReductionBuff;
    private final Map<RpgStatusType, RpgStatusInstance> statuses = new EnumMap<>(RpgStatusType.class);

    public boolean frontGuard() { return frontGuardTicks > 0 && guard > 0; }
    public boolean superArmor() { return superArmorTicks > 0; }
    public boolean iframe() { return iframeTicks > 0; }
    public boolean grabImmune() { return grabImmuneTicks > 0 || iframe(); }
    public int frontGuardTicks() { return frontGuardTicks; }
    public int superArmorTicks() { return superArmorTicks; }
    public int iframeTicks() { return iframeTicks; }
    public int grabImmuneTicks() { return grabImmuneTicks; }
    public double guard() { return guard; }
    public double maximumGuard() { return maximumGuard; }
    public double ccPoints() { return ccPoints; }
    public int ccImmunityTicks() { return ccImmunityTicks; }
    public CrowdControlType activeCc() { return activeCc; }
    public int activeCcTicks() { return activeCcTicks; }
    public boolean casting() { return casting; }
    public int manaShieldTicks() { return manaShieldTicks; }
    public double manaShieldRatio() { return manaShieldTicks > 0 ? manaShieldRatio : 0.0; }
    public int resistanceBuffTicks() { return resistanceBuffTicks; }
    public double resistanceBuff() { return resistanceBuffTicks > 0 ? resistanceBuff : 0.0; }
    public int damageReductionBuffTicks() { return damageReductionBuffTicks; }
    public double damageReductionBuff() { return damageReductionBuffTicks > 0 ? damageReductionBuff : 0.0; }
    public boolean downed() { return activeCc == CrowdControlType.KNOCKDOWN || activeCc == CrowdControlType.BOUND; }
    public boolean floated() { return activeCc == CrowdControlType.FLOAT; }
    public boolean frozen() { return activeCc == CrowdControlType.FREEZE && activeCcTicks > 0; }
    public boolean actionLocked() { return activeCc != null && (activeCcTicks > 0 || floated()); }
    public Map<RpgStatusType, RpgStatusInstance> statuses() { return statuses; }
    public double statusPotency(RpgStatusType type) {
        RpgStatusInstance status = statuses.get(type);
        return status == null ? 0.0 : status.potency() * status.stacks();
    }

    public void activateFrontGuard(int ticks) { frontGuardTicks = Math.max(frontGuardTicks, ticks); }
    public void activateSuperArmor(int ticks) { superArmorTicks = Math.max(superArmorTicks, ticks); }
    public void activateIframe(int ticks) { iframeTicks = Math.max(iframeTicks, ticks); }
    public void activateGrabImmunity(int ticks) { grabImmuneTicks = Math.max(grabImmuneTicks, ticks); }
    public void setCasting(boolean value) { casting = value; }
    public void activateManaShield(int ticks, double ratio) {
        if (ticks <= 0 || ratio <= 0) return;
        manaShieldTicks = Math.max(manaShieldTicks, ticks);
        manaShieldRatio = Math.max(manaShieldRatio, Math.min(1.0, ratio));
    }
    public void activateResistanceBuff(int ticks, double value) {
        if (ticks <= 0 || value <= 0) return;
        resistanceBuffTicks = Math.max(resistanceBuffTicks, ticks);
        resistanceBuff = Math.max(resistanceBuff, Math.min(1.0, value));
    }
    public void activateDamageReductionBuff(int ticks, double value) {
        if (ticks <= 0 || value <= 0) return;
        if (value > damageReductionBuff) {
            damageReductionBuff = Math.min(1.0, value);
            damageReductionBuffTicks = ticks;
        } else if (value == damageReductionBuff) {
            damageReductionBuffTicks = Math.max(damageReductionBuffTicks, ticks);
        }
    }

    public boolean absorbGuard(double damage) {
        if (!frontGuard()) return false;
        guard = Math.max(0.0, guard - Math.max(1.0, damage * CombatConfig.values().guardDamageScale()));
        if (guard == 0.0) frontGuardTicks = 0;
        return true;
    }

    public void applyCc(CrowdControlType type, int ticks, double points) {
        activeCc = type;
        activeCcTicks = Math.max(0, ticks);
        CombatConfig.Values config = CombatConfig.values();
        ccPoints = Math.min(config.maximumCcPoints(), ccPoints + points);
        ccChainResetTicks = config.ccChainResetTicks();
        if (ccPoints >= config.maximumCcPoints()) ccImmunityTicks = config.ccImmunityTicks();
    }

    public void applyImpulse(LivingEntity target, Vec3 origin) {
        applyImpulse(target, origin, CombatConfig.values().knockbackVelocity(),
                Math.max(0.1, target.getDeltaMovement().y));
    }

    public void applyImpulse(LivingEntity target, Vec3 origin, double horizontalVelocity, double verticalVelocity) {
        if (origin == null) return;
        Vec3 direction = target.position().subtract(origin).multiply(1, 0, 1);
        if (direction.lengthSqr() > 1.0E-6) {
            Vec3 impulse = direction.normalize().scale(horizontalVelocity);
            target.setDeltaMovement(impulse.x, verticalVelocity, impulse.z);
            target.hurtMarked = true;
        }
    }

    public void applySmash(SmashType type, int extensionTicks) {
        if (type == SmashType.DOWN_SMASH && downed()) activeCcTicks += Math.max(0, extensionTicks);
        if (type == SmashType.AIR_SMASH && floated()) airSmashLandingTicks = Math.max(0, extensionTicks);
    }

    public boolean tick(LivingEntity entity) {
        boolean transition = false;
        if (frontGuardTicks > 0 && --frontGuardTicks == 0) transition = true;
        if (superArmorTicks > 0 && --superArmorTicks == 0) transition = true;
        if (iframeTicks > 0 && --iframeTicks == 0) transition = true;
        if (grabImmuneTicks > 0 && --grabImmuneTicks == 0) transition = true;
        if (manaShieldTicks > 0 && --manaShieldTicks == 0) {
            manaShieldRatio = 0.0;
            transition = true;
        }
        if (resistanceBuffTicks > 0 && --resistanceBuffTicks == 0) {
            resistanceBuff = 0.0;
            transition = true;
        }
        if (damageReductionBuffTicks > 0 && --damageReductionBuffTicks == 0) {
            damageReductionBuff = 0.0;
            transition = true;
        }
        if (ccImmunityTicks > 0 && --ccImmunityTicks == 0) {
            ccPoints = 0.0;
            transition = true;
        }
        if (ccChainResetTicks > 0 && ccImmunityTicks == 0 && --ccChainResetTicks == 0) {
            ccPoints = 0.0;
            transition = true;
        }
        if (activeCcTicks > 0) activeCcTicks--;
        if (activeCc == CrowdControlType.FLOAT && airSmashLandingTicks > 0 && entity.onGround()) {
            activeCc = CrowdControlType.KNOCKDOWN;
            activeCcTicks = airSmashLandingTicks;
            airSmashLandingTicks = 0;
            transition = true;
        }
        if (activeCc != null && activeCcTicks == 0 && (activeCc != CrowdControlType.FLOAT || entity.onGround())) {
            activeCc = null;
            transition = true;
        }

        if (actionLocked()) {
            if (entity instanceof Mob mob) mob.getNavigation().stop();
            if (activeCc != CrowdControlType.FLOAT && activeCc != CrowdControlType.KNOCKBACK) {
                Vec3 velocity = entity.getDeltaMovement();
                entity.setDeltaMovement(0.0, velocity.y, 0.0);
            }
        }
        if (!frontGuard() && !casting) {
            guard = Math.min(maximumGuard, guard + CombatConfig.values().guardRegenPerTick());
        }
        return transition;
    }

    public void clear() {
        frontGuardTicks = superArmorTicks = iframeTicks = grabImmuneTicks = 0;
        ccImmunityTicks = ccChainResetTicks = activeCcTicks = 0;
        ccPoints = 0.0;
        activeCc = null;
        casting = false;
        airSmashLandingTicks = 0;
        manaShieldTicks = resistanceBuffTicks = damageReductionBuffTicks = 0;
        manaShieldRatio = resistanceBuff = damageReductionBuff = 0.0;
        statuses.clear();
    }

    public void applyClientSnapshot(double guard, double maximumGuard, double ccPoints, int immunityTicks,
                                    CrowdControlType activeCc, int activeTicks, boolean casting) {
        this.guard = guard;
        this.maximumGuard = maximumGuard;
        this.ccPoints = ccPoints;
        this.ccImmunityTicks = immunityTicks;
        this.activeCc = activeCc;
        this.activeCcTicks = activeTicks;
        this.casting = casting;
    }
}
