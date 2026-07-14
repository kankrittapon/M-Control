package com.zexqm.rpgproject.gametest;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.rpg.CrowdControlType;
import com.zexqm.rpgproject.rpg.combat.CrowdControlApplicationResult;
import com.zexqm.rpgproject.rpg.combat.CrowdControlResolver;
import com.zexqm.rpgproject.rpg.combat.RpgCombatStateProvider;
import com.zexqm.rpgproject.rpg.combat.RpgCombatService;
import com.zexqm.rpgproject.rpg.combat.RpgDamageContext;
import com.zexqm.rpgproject.rpg.combat.RpgDamageResult;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import com.zexqm.rpgproject.rpg.mob.MobControlProfile;
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
import net.minecraftforge.gametest.GameTestHolder;

import java.util.Collections;
import java.util.Set;

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

    private static RpgDamageContext context(Zombie attacker, Zombie target, double damage,
                                            CrowdControlType crowdControl) {
        return new RpgDamageContext(attacker, target, attacker.position(), damage, RpgPowerType.NONE,
                1.0, false, false, crowdControl, Collections.emptySet());
    }

    private WorldCoreGameTests() {}
}
