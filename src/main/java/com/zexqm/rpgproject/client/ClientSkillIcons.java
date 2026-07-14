package com.zexqm.rpgproject.client;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.resources.ResourceLocation;

public final class ClientSkillIcons {
    private static final ResourceLocation FIREBALL_ID = new ResourceLocation(RpgProject.MOD_ID, "wizard_fireball");
    private static final ResourceLocation FIREBALL = new ResourceLocation(RpgProject.MOD_ID,
            "textures/gui/skills/wizard/main/fireball.png");
    private static final ResourceLocation FIREBALL_EXPLOSION_ID = new ResourceLocation(
            RpgProject.MOD_ID, "wizard_fireball_explosion");
    private static final ResourceLocation FIREBALL_EXPLOSION = new ResourceLocation(RpgProject.MOD_ID,
            "textures/gui/skills/wizard/main/fireball_explosion.png");

    public static ResourceLocation icon(ResourceLocation skill) {
        if (FIREBALL_ID.equals(skill)) return FIREBALL;
        if (FIREBALL_EXPLOSION_ID.equals(skill)) return FIREBALL_EXPLOSION;
        return null;
    }

    private ClientSkillIcons() {}
}
