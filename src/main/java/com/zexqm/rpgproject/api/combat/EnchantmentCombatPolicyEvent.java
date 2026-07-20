package com.zexqm.rpgproject.api.combat;

import com.zexqm.rpgproject.rpg.combat.enchant.EnchantmentCombatPolicy;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

public final class EnchantmentCombatPolicyEvent extends Event {
    private final ResourceLocation enchantmentId;
    private EnchantmentCombatPolicy policy;

    public EnchantmentCombatPolicyEvent(ResourceLocation enchantmentId, EnchantmentCombatPolicy policy) {
        this.enchantmentId = enchantmentId;
        this.policy = policy;
    }

    public ResourceLocation enchantmentId() { return enchantmentId; }
    public EnchantmentCombatPolicy policy() { return policy; }
    public void setPolicy(EnchantmentCombatPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("Enchantment policy is required");
        this.policy = policy;
    }
}
