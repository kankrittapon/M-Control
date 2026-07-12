package com.zexqm.rpgproject.rpg;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID)
public final class DamagePipeline {
    private static final double ATTACK_POWER_SCALE = 0.10;

    @SubscribeEvent
    public static void accuracyCheck(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        boolean iframe = target.getCapability(RpgPlayerDataProvider.DATA)
                .map(data -> data.protection().iframe()).orElse(false);
        if (iframe) { event.setCanceled(true); return; }
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        if (attacker == null) return;

        double accuracy = attacker.getAttribute(ModAttributes.ACCURACY.get()) == null
                ? 100.0 : attacker.getAttributeValue(ModAttributes.ACCURACY.get());
        double evasion = target.getAttributeValue(ModAttributes.EVASION.get());
        double hitChance = Mth.clamp(accuracy / Math.max(1.0, accuracy + evasion), 0.10, 0.95);
        if (target.getRandom().nextDouble() > hitChance) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void resolveDamage(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        float damage = event.getAmount();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            damage += (float) (attacker.getAttributeValue(ModAttributes.ATTACK_POWER.get()) * ATTACK_POWER_SCALE);
            if (attacker.getRandom().nextDouble() < attacker.getAttributeValue(ModAttributes.CRITICAL_CHANCE.get()))
                damage *= (float) attacker.getAttributeValue(ModAttributes.CRITICAL_DAMAGE.get());
        }
        if (event.getEntity() instanceof ServerPlayer target
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            final float guardDamage = damage;
            boolean guarded = target.getCapability(RpgPlayerDataProvider.DATA).map(data -> {
                if (!CrowdControlResolver.isFrontal(target, event.getSource().getSourcePosition())) return false;
                return data.protection().absorbGuard(guardDamage);
            }).orElse(false);
            if (guarded) { event.setAmount(0); return; }
            double reduction = Mth.clamp(target.getAttributeValue(ModAttributes.DAMAGE_REDUCTION.get()), 0.0, 0.80);
            damage *= (float) (1.0 - reduction);
        }
        event.setAmount(Math.max(0.0F, damage));
    }

    private DamagePipeline() {}
}
