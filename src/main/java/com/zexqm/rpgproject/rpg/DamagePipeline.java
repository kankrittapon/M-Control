package com.zexqm.rpgproject.rpg;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.rpg.combat.CombatConfig;
import com.zexqm.rpgproject.rpg.combat.RpgCombatMath;
import com.zexqm.rpgproject.rpg.combat.RpgCombatService;
import com.zexqm.rpgproject.rpg.combat.RpgCombatStateProvider;
import com.zexqm.rpgproject.rpg.combat.ManaShieldService;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID)
public final class DamagePipeline {
    @SubscribeEvent
    public static void protectAndLock(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide || RpgCombatService.applyingTaggedDamage()) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            boolean locked = attacker.getCapability(RpgCombatStateProvider.DATA)
                    .map(state -> state.actionLocked()).orElse(false);
            if (locked) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        boolean protectedState = event.getEntity().getCapability(RpgCombatStateProvider.DATA)
                .map(state -> state.iframe() || state.frozen()).orElse(false);
        if (protectedState) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void vanillaFrontalGuard(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || RpgCombatService.applyingTaggedDamage()
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getSource().getEntity() == null || event.getSource().getSourcePosition() == null) return;
        boolean guarded = event.getEntity().getCapability(RpgCombatStateProvider.DATA).map(state ->
                state.frontGuard()
                        && RpgCombatMath.withinFacingArc(event.getEntity(), event.getSource().getSourcePosition(),
                        CombatConfig.values().frontalGuardArcDegrees())
                        && state.absorbGuard(event.getAmount())).orElse(false);
        if (guarded) event.setAmount(0.0F);
    }

    @SubscribeEvent
    public static void vanillaManaShield(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || RpgCombatService.applyingTaggedDamage()
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getAmount() <= 0) return;
        event.setAmount(ManaShieldService.absorb(event.getEntity(), event.getAmount()).remainingDamage());
    }

    private DamagePipeline() {}
}
