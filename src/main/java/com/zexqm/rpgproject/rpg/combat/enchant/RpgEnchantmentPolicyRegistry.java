package com.zexqm.rpgproject.rpg.combat.enchant;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.MinecraftForge;
import com.zexqm.rpgproject.api.combat.EnchantmentCombatPolicyEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgEnchantmentPolicyRegistry {
    private static final Map<ResourceLocation, EnchantmentCombatPolicy> POLICIES = new ConcurrentHashMap<>();

    static {
        disableInRpgCombat("sharpness");
        disableInRpgCombat("smite");
        disableInRpgCombat("bane_of_arthropods");
        disableInRpgCombat("fire_aspect");
        disableInRpgCombat("knockback");
        disableInRpgCombat("sweeping");
    }

    public static EnchantmentCombatPolicy policy(Enchantment enchantment) {
        return policy(BuiltInRegistries.ENCHANTMENT.getKey(enchantment));
    }

    public static EnchantmentCombatPolicy policy(ResourceLocation enchantmentId) {
        EnchantmentCombatPolicyEvent event = new EnchantmentCombatPolicyEvent(enchantmentId,
                POLICIES.getOrDefault(enchantmentId, EnchantmentCombatPolicy.VANILLA_ONLY));
        MinecraftForge.EVENT_BUS.post(event);
        return event.policy();
    }

    public static void register(ResourceLocation enchantmentId, EnchantmentCombatPolicy policy) {
        if (enchantmentId == null || policy == null) throw new IllegalArgumentException("Enchantment ID and policy are required");
        POLICIES.put(enchantmentId, policy);
    }

    private static void disableInRpgCombat(String path) {
        register(new ResourceLocation("minecraft", path), EnchantmentCombatPolicy.DISABLED_IN_RPG_COMBAT);
    }

    private RpgEnchantmentPolicyRegistry() {}
}
