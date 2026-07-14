package com.zexqm.rpgproject.rpg.status;

import com.zexqm.rpgproject.rpg.combat.RpgCombatService;
import com.zexqm.rpgproject.rpg.combat.RpgCombatStateProvider;
import com.zexqm.rpgproject.rpg.combat.RpgDamageContext;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import com.zexqm.rpgproject.rpg.mob.MobControlProfiles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import java.util.Set;

public final class RpgStatusService {
    private static final UUID SLOW_MODIFIER = UUID.fromString("d3c259da-95cb-42a3-a6af-35bcdf97acbd");

    public static StatusApplicationResult apply(LivingEntity source, LivingEntity target, RpgStatusType type,
                                                int durationTicks, int intervalTicks, double potency,
                                                int maxStacks, StatusStackingPolicy stacking) {
        return apply(source, target, type, durationTicks, intervalTicks, potency, maxStacks, stacking, Set.of());
    }

    public static StatusApplicationResult apply(LivingEntity source, LivingEntity target, RpgStatusType type,
                                                int durationTicks, int intervalTicks, double potency,
                                                int maxStacks, StatusStackingPolicy stacking,
                                                Set<MobControlProfile> allowedProfiles) {
        if (source == null || target == null || type == null || durationTicks <= 0) return StatusApplicationResult.INVALID;
        MobControlProfile profile = MobControlProfiles.resolve(target);
        if (profile.statusImmune() || profile == MobControlProfile.BOSS && !allowedProfiles.contains(profile))
            return StatusApplicationResult.IMMUNE;
        int adjustedDuration = Math.max(1, (int) Math.round(durationTicks * profile.durationMultiplier()));
        return target.getCapability(RpgCombatStateProvider.DATA).map(state -> {
            RpgStatusInstance incoming = new RpgStatusInstance(type, source.getUUID(), adjustedDuration,
                    intervalTicks, potency, maxStacks, stacking);
            RpgStatusInstance existing = state.statuses().get(type);
            if (existing == null) {
                state.statuses().put(type, incoming);
                return StatusApplicationResult.APPLIED;
            }
            existing.merge(incoming);
            return StatusApplicationResult.REFRESHED;
        }).orElse(StatusApplicationResult.INVALID);
    }

    public static boolean tick(LivingEntity target) {
        var state = target.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (state == null || state.statuses().isEmpty()) return false;
        boolean changed = false;
        Iterator<RpgStatusInstance> iterator = state.statuses().values().iterator();
        while (iterator.hasNext()) {
            RpgStatusInstance status = iterator.next();
            if (status.tickInterval()) applyPeriodic(target, status);
            if (status.expired()) {
                iterator.remove();
                changed = true;
            }
        }
        updateSlow(target, state.statusPotency(RpgStatusType.SLOW));
        return changed;
    }

    private static void applyPeriodic(LivingEntity target, RpgStatusInstance status) {
        if (status.type() != RpgStatusType.BURN || !(target.level() instanceof ServerLevel level)) return;
        if (!(level.getEntity(status.source()) instanceof LivingEntity source) || !source.isAlive()) return;
        RpgCombatService.apply(new RpgDamageContext(source, target, source.position(),
                status.potency() * status.stacks(), RpgPowerType.MAGIC, 1.0,
                false, false, null, Collections.emptySet()));
    }

    private static void updateSlow(LivingEntity target, double potency) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) return;
        movement.removeModifier(SLOW_MODIFIER);
        if (potency > 0) {
            movement.addTransientModifier(new AttributeModifier(SLOW_MODIFIER, "RPG slow",
                    -Math.min(0.9, potency), AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private RpgStatusService() {}
}
