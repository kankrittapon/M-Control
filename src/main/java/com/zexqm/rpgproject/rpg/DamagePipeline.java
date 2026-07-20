package com.zexqm.rpgproject.rpg;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.rpg.combat.CombatImpactContext;
import com.zexqm.rpgproject.rpg.combat.CombatConfig;
import com.zexqm.rpgproject.rpg.combat.ProtectionDecision;
import com.zexqm.rpgproject.rpg.combat.ProtectionResolver;
import com.zexqm.rpgproject.rpg.combat.RpgCombatService;
import com.zexqm.rpgproject.rpg.combat.RpgCombatStateProvider;
import com.zexqm.rpgproject.rpg.combat.ManaShieldService;
import com.zexqm.rpgproject.rpg.combat.TimedDamageReductionService;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID)
public final class DamagePipeline {
    @SubscribeEvent
    public static void replaceVanillaCombatAttack(AttackEntityEvent event) {
        if (!com.zexqm.rpgproject.rpg.combat.RpgCombatModeRules.replacesVanillaAttack(event.getEntity())) return;
        event.setCanceled(true);
        RpgProject.LOGGER.info("[RPG Enchant Policy] vanilla-attack blocked player={} target={} reason=rpg-combat-mode",
                event.getEntity().getScoreboardName(), event.getTarget().getType().toShortString());
    }

    @SubscribeEvent
    public static void protectionPreventsVanillaKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        CombatImpactContext impact = CombatImpactContext.fromKnockback(event.getEntity(),
                event.getOriginalRatioX(), event.getOriginalRatioZ(), event.getOriginalStrength());
        ProtectionDecision decision = event.getEntity().getCapability(RpgCombatStateProvider.DATA)
                .map(state -> ProtectionResolver.resolveKnockback(state, impact))
                .orElse(ProtectionDecision.NONE);
        if (decision.blockKnockback()) {
            event.setCanceled(true);
            if (com.zexqm.rpgproject.rpg.combat.CombatConfig.values().logImpactDecisions())
                RpgProject.LOGGER.info("[RPG Impact] knockback target={} category={} origin={} strength={} protection={} blocked=true",
                        event.getEntity().getScoreboardName(), impact.category(), impact.origin(),
                        event.getOriginalStrength(), decision.reason());
        }
    }

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
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        CombatImpactContext impact = CombatImpactContext.fromDamage(event.getEntity(), event.getSource(),
                event.getAmount(), false);
        var state = event.getEntity().getCapability(RpgCombatStateProvider.DATA).orElse(null);
        ProtectionDecision decision = state == null ? ProtectionDecision.NONE
                : ProtectionResolver.resolveDamage(state, impact);
        boolean protectionActive = state != null
                && (state.frontGuard() || state.superArmor() || state.iframe());
        boolean guarded = decision.blockDamage()
                && (!decision.consumeGuard() || state.absorbGuard(event.getAmount()));
        if (CombatConfig.values().logImpactDecisions() && protectionActive)
            RpgProject.LOGGER.info("[RPG Impact] damage target={} category={} origin={} incoming={} protection={} blocked={}",
                    event.getEntity().getScoreboardName(), impact.category(), impact.origin(),
                    impact.incomingDamage(), decision.reason(), guarded);
        if (guarded) {
            event.setAmount(0.0F);
        }
    }

    @SubscribeEvent
    public static void vanillaManaShield(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || RpgCombatService.applyingTaggedDamage()
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getAmount() <= 0) return;
        float reduced = TimedDamageReductionService.reduce(event.getEntity(), event.getAmount());
        event.setAmount(ManaShieldService.absorb(event.getEntity(), reduced).remainingDamage());
    }

    private DamagePipeline() {}
}
