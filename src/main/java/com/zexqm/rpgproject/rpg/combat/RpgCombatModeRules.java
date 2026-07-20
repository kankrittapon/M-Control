package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.rpg.RpgPlayerDataProvider;
import net.minecraft.world.entity.player.Player;

public final class RpgCombatModeRules {
    public static boolean replacesVanillaAttack(Player player) {
        return player.getCapability(RpgPlayerDataProvider.DATA)
                .map(data -> data.weaponDrawn() || data.inCombat()).orElse(false);
    }

    private RpgCombatModeRules() {}
}
