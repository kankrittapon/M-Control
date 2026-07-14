package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.rpg.RpgClass;

public enum PrimaryResourceType {
    MP,
    WP;

    public static PrimaryResourceType forClass(RpgClass rpgClass) {
        return rpgClass == RpgClass.NINJA ? WP : MP;
    }
}
