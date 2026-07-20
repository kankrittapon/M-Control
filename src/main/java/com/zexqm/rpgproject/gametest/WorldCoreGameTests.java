package com.zexqm.rpgproject.gametest;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.api.combat.CombatImpactResolveEvent;
import com.zexqm.rpgproject.api.combat.EnchantmentCombatPolicyEvent;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.combat.CrowdControlApplicationResult;
import com.zexqm.rpgproject.rpg.combat.CrowdControlResolver;
import com.zexqm.rpgproject.rpg.combat.CombatConfig;
import com.zexqm.rpgproject.rpg.combat.RpgCombatStateProvider;
import com.zexqm.rpgproject.rpg.combat.RpgCombatService;
import com.zexqm.rpgproject.rpg.combat.RpgDamageContext;
import com.zexqm.rpgproject.rpg.combat.RpgDamageResult;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import com.zexqm.rpgproject.rpg.combat.SmashApplicationResult;
import com.zexqm.rpgproject.rpg.combat.SmashResolver;
import com.zexqm.rpgproject.rpg.combat.SmashType;
import com.zexqm.rpgproject.rpg.combat.CombatImpactCategory;
import com.zexqm.rpgproject.rpg.combat.CombatImpactContext;
import com.zexqm.rpgproject.rpg.combat.CombatImpactDiagnostics;
import com.zexqm.rpgproject.rpg.combat.ProtectionDecision;
import com.zexqm.rpgproject.rpg.combat.ProtectionResolver;
import com.zexqm.rpgproject.rpg.combat.enchant.EnchantmentCombatPolicy;
import com.zexqm.rpgproject.rpg.combat.enchant.RpgEnchantmentPolicyRegistry;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
import com.zexqm.rpgproject.rpg.mob.MobControlProfileEvent;
import com.zexqm.rpgproject.rpg.mob.MobControlProfiles;
import com.zexqm.rpgproject.rpg.status.RpgStatusService;
import com.zexqm.rpgproject.rpg.status.RpgStatusType;
import com.zexqm.rpgproject.rpg.status.StatusApplicationResult;
import com.zexqm.rpgproject.rpg.status.StatusStackingPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

@GameTestHolder(RpgProject.MOD_ID)
public final class WorldCoreGameTests {
    @GameTest(template = "empty")
    public static void livingEntitiesReceiveCombatState(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        helper.assertTrue(zombie.getCapability(RpgCombatStateProvider.DATA).isPresent(),
                "LivingEntity is missing RPG combat state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void superArmorBlocksCrowdControl(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        zombie.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state")).activateSuperArmor(20);
        CrowdControlApplicationResult result = CrowdControlResolver.apply(zombie, CrowdControlType.STUN,
                zombie.position().add(0, 0, 2));
        helper.assertTrue(result.status() == CrowdControlApplicationResult.Status.SUPER_ARMOR,
                "Super Armor did not block stun");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void superArmorBlocksVanillaKnockback(GameTestHelper helper) {
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        target.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"))
                .activateSuperArmor(20);
        LivingKnockBackEvent event = new LivingKnockBackEvent(target, 0.4F, 0, 1);
        MinecraftForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled(), "Super Armor did not block Vanilla knockback");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void perfectGuardUsesFrontGuardAndRearSuperArmor(GameTestHelper helper) {
        Zombie frontAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        Zombie rearAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        target.setYRot(0.0F);
        var state = target.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"));
        state.activateFrontGuard(20);
        state.activateSuperArmor(20);

        RpgDamageResult front = RpgCombatService.apply(context(frontAttacker, target, 5, CrowdControlType.STUN));
        RpgDamageResult rear = RpgCombatService.apply(context(rearAttacker, target, 5, CrowdControlType.STUN));
        helper.assertTrue(front.outcome() == RpgDamageResult.Outcome.GUARDED,
                "Perfect Guard did not guard frontal damage");
        helper.assertTrue(rear.outcome() == RpgDamageResult.Outcome.HIT
                        && rear.crowdControl().status() == CrowdControlApplicationResult.Status.SUPER_ARMOR,
                "Perfect Guard did not allow rear damage while Super Armor blocked rear CC");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crowdControlBudgetStartsImmunity(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        CrowdControlResolver.apply(zombie, CrowdControlType.STUN, zombie.position().add(0, 0, 2));
        CrowdControlResolver.apply(zombie, CrowdControlType.KNOCKDOWN, zombie.position().add(0, 0, 2));
        var state = zombie.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"));
        helper.assertTrue(state.ccPoints() == 2.0 && state.ccImmunityTicks() == 100,
                "CC budget did not start immunity at 2.0 points");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void downSmashIgnoresCcImmunityWithoutAddingBudget(GameTestHelper helper) {
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        CrowdControlResolver.apply(target, CrowdControlType.STUN, attacker.position());
        CrowdControlResolver.apply(target, CrowdControlType.KNOCKDOWN, attacker.position());
        var state = target.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"));
        int beforeTicks = state.activeCcTicks();
        double beforePoints = state.ccPoints();
        SmashApplicationResult result = SmashResolver.apply(target, SmashType.DOWN_SMASH, 1.0,
                attacker.position());
        helper.assertTrue(result.status() == SmashApplicationResult.Status.APPLIED
                        && state.ccImmunityTicks() > 0 && state.ccPoints() == beforePoints
                        && state.activeCcTicks() == beforeTicks + CombatConfig.values().downSmashExtensionTicks(),
                "Down Smash used CC budget, respected immunity, or failed to extend down state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smashRequiresMatchingControlState(GameTestHelper helper) {
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        SmashApplicationResult result = SmashResolver.apply(target, SmashType.AIR_SMASH, 1.0,
                attacker.position());
        helper.assertTrue(result.status() == SmashApplicationResult.Status.WRONG_STATE,
                "Air Smash applied to a target that was not floating");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void iframeBlocksTaggedDamage(GameTestHelper helper) {
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        target.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state")).activateIframe(20);
        float health = target.getHealth();
        RpgDamageResult result = RpgCombatService.apply(context(attacker, target, 5, null));
        helper.assertTrue(result.outcome() == RpgDamageResult.Outcome.IFRAME && target.getHealth() == health,
                "Iframe did not block tagged damage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void frozenTargetsRejectAdditionalDamage(GameTestHelper helper) {
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        CrowdControlResolver.apply(target, CrowdControlType.FREEZE, attacker.position());
        float health = target.getHealth();
        RpgDamageResult result = RpgCombatService.apply(context(attacker, target, 5, null));
        helper.assertTrue(result.outcome() == RpgDamageResult.Outcome.FROZEN_IMMUNE
                        && target.getHealth() == health,
                "Frozen target accepted additional damage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taggedDamageIsAppliedOnce(GameTestHelper helper) {
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        float health = target.getHealth();
        RpgDamageResult result = RpgCombatService.apply(context(attacker, target, 5, null));
        helper.assertTrue(result.outcome() == RpgDamageResult.Outcome.HIT
                        && Math.abs(target.getHealth() - (health - 5.0F)) < 0.001F,
                "Tagged damage was skipped or applied more than once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void frontalGuardBlocksDamageAndCcBudget(GameTestHelper helper) {
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        target.setYRot(0.0F);
        var state = target.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"));
        state.activateFrontGuard(20);
        RpgDamageResult result = RpgCombatService.apply(context(attacker, target, 5, CrowdControlType.STUN));
        helper.assertTrue(result.outcome() == RpgDamageResult.Outcome.GUARDED && state.ccPoints() == 0.0,
                "Frontal Guard did not block damage and CC budget");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unknownLivingEntityDefaultsToNormal(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        helper.assertTrue(MobControlProfiles.resolve(zombie) == MobControlProfile.NORMAL,
                "Untagged LivingEntity did not fall back to NORMAL");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bossRejectsHardCrowdControl(GameTestHelper helper) {
        var wither = helper.spawn(EntityType.WITHER, new BlockPos(2, 3, 2));
        CrowdControlApplicationResult result = CrowdControlResolver.apply(wither, CrowdControlType.STUN,
                wither.position().add(0, 0, 2));
        helper.assertTrue(MobControlProfiles.resolve(wither) == MobControlProfile.BOSS
                        && result.status() == CrowdControlApplicationResult.Status.IMMUNE,
                "Boss profile accepted hard crowd control");
        wither.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bossStatusRequiresWhitelistAndUsesReducedDuration(GameTestHelper helper) {
        Zombie source = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        var wither = helper.spawn(EntityType.WITHER, new BlockPos(2, 3, 2));
        StatusApplicationResult rejected = RpgStatusService.apply(source, wither, RpgStatusType.BURN,
                100, 20, 1, 1, StatusStackingPolicy.REFRESH, Set.of(MobControlProfile.NORMAL));
        StatusApplicationResult applied = RpgStatusService.apply(source, wither, RpgStatusType.BURN,
                100, 20, 1, 1, StatusStackingPolicy.REFRESH, Set.of(MobControlProfile.BOSS));
        var state = wither.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"));
        helper.assertTrue(rejected == StatusApplicationResult.IMMUNE
                        && applied == StatusApplicationResult.APPLIED
                        && state.statuses().get(RpgStatusType.BURN).remainingTicks() == 50,
                "Boss status whitelist or duration multiplier is incorrect");
        wither.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void eliteStatusUsesReducedDuration(GameTestHelper helper) {
        Zombie source = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        var warden = helper.spawn(EntityType.WARDEN, new BlockPos(2, 3, 2));
        StatusApplicationResult result = RpgStatusService.apply(source, warden, RpgStatusType.SLOW,
                100, 20, 0.2, 1, StatusStackingPolicy.REFRESH, Set.of(MobControlProfile.ELITE));
        var state = warden.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"));
        helper.assertTrue(MobControlProfiles.resolve(warden) == MobControlProfile.ELITE
                        && result == StatusApplicationResult.APPLIED
                        && state.statuses().get(RpgStatusType.SLOW).remainingTicks() == 60,
                "Elite profile did not apply its status duration multiplier");
        warden.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void compatibilityOverrideCanMakeEntityUnstoppable(GameTestHelper helper) {
        Zombie source = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        Consumer<MobControlProfileEvent> override = event -> {
            if (event.entity() == target) event.setProfile(MobControlProfile.UNSTOPPABLE);
        };
        MinecraftForge.EVENT_BUS.addListener(override);
        try {
            CrowdControlApplicationResult crowdControl = CrowdControlResolver.apply(target,
                    CrowdControlType.STUN, source.position());
            StatusApplicationResult status = RpgStatusService.apply(source, target, RpgStatusType.BURN,
                    100, 20, 1, 1, StatusStackingPolicy.REFRESH,
                    Set.of(MobControlProfile.UNSTOPPABLE));
            helper.assertTrue(MobControlProfiles.resolve(target) == MobControlProfile.UNSTOPPABLE
                            && crowdControl.status() == CrowdControlApplicationResult.Status.IMMUNE
                            && status == StatusApplicationResult.IMMUNE,
                    "Compatibility override did not block harmful CC and Status");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(override);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void compatibilityCanOverrideUnknownImpactAndEnchantment(GameTestHelper helper) {
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        target.setYRot(0.0F);
        var state = target.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"));
        state.activateFrontGuard(20);
        ResourceLocation enchantmentId = new ResourceLocation("compat_test", "impact_power");
        Consumer<CombatImpactResolveEvent> impactOverride = event -> {
            if (event.target() == target && event.source() == null) {
                event.setCategory(CombatImpactCategory.EXPLOSION);
                event.setOrigin(target.position().add(0, 0, 2));
            }
        };
        Consumer<EnchantmentCombatPolicyEvent> enchantmentOverride = event -> {
            if (event.enchantmentId().equals(enchantmentId))
                event.setPolicy(EnchantmentCombatPolicy.RPG_BRIDGED);
        };
        MinecraftForge.EVENT_BUS.addListener(impactOverride);
        MinecraftForge.EVENT_BUS.addListener(enchantmentOverride);
        try {
            CombatImpactContext impact = CombatImpactContext.fromKnockback(target, 0, 0, 0.4F);
            helper.assertTrue(impact.category() == CombatImpactCategory.EXPLOSION
                            && ProtectionResolver.resolveKnockback(state, impact).blockKnockback()
                            && RpgEnchantmentPolicyRegistry.policy(enchantmentId)
                            == EnchantmentCombatPolicy.RPG_BRIDGED,
                    "Compatibility API did not override impact origin/category or enchantment policy");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(impactOverride);
            MinecraftForge.EVENT_BUS.unregister(enchantmentOverride);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void impactDiagnosticsExposePerfectGuardDirectionsAndGuardCost(GameTestHelper helper) {
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        target.setYRot(0.0F);
        var state = target.getCapability(RpgCombatStateProvider.DATA)
                .orElseThrow(() -> new IllegalStateException("Missing combat state"));
        state.activateFrontGuard(20);
        state.activateSuperArmor(20);
        Vec3 front = target.position().add(0, 0, 2);
        Vec3 rear = target.position().add(0, 0, -2);
        var frontResult = CombatImpactDiagnostics.probe(state, CombatImpactContext.resolve(target,
                null, 10.0F, CombatImpactCategory.DIRECT, front, false, null, null, false));
        var rearResult = CombatImpactDiagnostics.inspect(state, CombatImpactContext.resolve(target,
                null, 10.0F, CombatImpactCategory.EXPLOSION, rear, false, null, null, false));
        helper.assertTrue(frontResult.perfectGuard()
                        && frontResult.damageDecision().reason() == ProtectionDecision.Reason.PERFECT_GUARD
                        && frontResult.damageAfter() == 0.0F
                        && frontResult.guardBefore() - frontResult.guardAfter() == 10.0
                        && frontResult.knockbackDecision().blockKnockback()
                        && rearResult.damageDecision().reason() == ProtectionDecision.Reason.SUPER_ARMOR
                        && rearResult.damageAfter() == 10.0F
                        && rearResult.knockbackDecision().blockKnockback(),
                "Impact diagnostics did not expose Perfect Guard front/rear behavior");
        helper.succeed();
    }

    private static RpgDamageContext context(Zombie attacker, Zombie target, double damage,
                                            CrowdControlType crowdControl) {
        return new RpgDamageContext(attacker, target, attacker.position(), damage, RpgPowerType.NONE,
                1.0, false, false, crowdControl, Collections.emptySet());
    }

    private WorldCoreGameTests() {}
}
