package com.zexqm.rpgproject.rpg;

import net.minecraft.world.item.Item;

public class RpgWeaponItem extends Item {
    private final RpgClass rpgClass;
    private final WeaponSlot slot;
    private final WeaponType type;

    public RpgWeaponItem(RpgClass rpgClass, WeaponSlot slot, WeaponType type, Properties properties) {
        super(properties);
        this.rpgClass = rpgClass;
        this.slot = slot;
        this.type = type;
    }

    public RpgClass rpgClass() { return rpgClass; }
    public WeaponSlot slot() { return slot; }
    public WeaponType type() { return type; }
}
