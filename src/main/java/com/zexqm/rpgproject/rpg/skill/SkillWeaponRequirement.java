package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.rpg.*;

public record SkillWeaponRequirement(WeaponSet weaponSet, boolean main, boolean sub, boolean awakening) {
    public boolean test(RpgPlayerData data) {
        if (data.activeSet() != weaponSet) return false;
        if (main && data.weapon(WeaponSlot.MAIN).isEmpty()) return false;
        if (sub && data.weapon(WeaponSlot.SUB).isEmpty()) return false;
        return !awakening || !data.weapon(WeaponSlot.AWAKENING).isEmpty();
    }
}
