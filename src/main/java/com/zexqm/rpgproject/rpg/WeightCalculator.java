package com.zexqm.rpgproject.rpg;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public final class WeightCalculator {
    public static double capacity(RpgPlayerData data) { return 100.0 + data.strength().level() * 5.0; }

    public static double carried(Player player) {
        double total = 0;
        for (ItemStack stack : player.getInventory().items) total += stackWeight(stack);
        for (ItemStack stack : player.getInventory().armor) total += stackWeight(stack);
        for (ItemStack stack : player.getInventory().offhand) total += stackWeight(stack);
        return total;
    }

    public static double loadRatio(Player player, RpgPlayerData data) {
        return carried(player) / Math.max(1.0, capacity(data));
    }

    private static double stackWeight(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        double each = stack.getItem() instanceof RpgWeaponItem ? 5.0
                : stack.getItem() instanceof ArmorItem ? 4.0 : 0.25;
        return each * stack.getCount();
    }
    private WeightCalculator() {}
}
