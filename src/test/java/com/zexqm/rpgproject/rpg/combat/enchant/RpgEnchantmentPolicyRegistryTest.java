package com.zexqm.rpgproject.rpg.combat.enchant;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RpgEnchantmentPolicyRegistryTest {
    @Test void offensiveVanillaEnchantmentsCannotLeakIntoRpgAttacks() {
        assertEquals(EnchantmentCombatPolicy.DISABLED_IN_RPG_COMBAT,
                RpgEnchantmentPolicyRegistry.policy(id("sharpness")));
        assertEquals(EnchantmentCombatPolicy.DISABLED_IN_RPG_COMBAT,
                RpgEnchantmentPolicyRegistry.policy(id("fire_aspect")));
        assertEquals(EnchantmentCombatPolicy.DISABLED_IN_RPG_COMBAT,
                RpgEnchantmentPolicyRegistry.policy(id("knockback")));
    }

    @Test void unrelatedEnchantmentsRemainVanillaOnly() {
        assertEquals(EnchantmentCombatPolicy.VANILLA_ONLY,
                RpgEnchantmentPolicyRegistry.policy(id("projectile_protection")));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
