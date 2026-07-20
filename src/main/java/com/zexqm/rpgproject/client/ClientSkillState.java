package com.zexqm.rpgproject.client;

import com.zexqm.rpgproject.rpg.skill.MovementPolicy;
import com.zexqm.rpgproject.rpg.skill.PrimaryResourceType;
import com.zexqm.rpgproject.rpg.skill.SkillActionState;
import com.zexqm.rpgproject.rpg.skill.SkillAimMode;
import net.minecraft.resources.ResourceLocation;

public final class ClientSkillState {
    private static SkillActionState action = SkillActionState.SHEATHED;
    private static MovementPolicy movement = MovementPolicy.FULL;
    private static PrimaryResourceType resource = PrimaryResourceType.MP;
    private static ResourceLocation activeSkill;
    private static int actionTicks;
    private static int castTicks;
    private static boolean movementCancelAllowed;
    private static SkillAimMode aimMode = SkillAimMode.INSTANT_AIM;

    public static void apply(SkillActionState value, MovementPolicy policy, PrimaryResourceType resourceType,
                             ResourceLocation skill, int remainingTicks, int totalCastTicks) {
        apply(value, policy, resourceType, skill, remainingTicks, totalCastTicks, false);
    }

    public static void apply(SkillActionState value, MovementPolicy policy, PrimaryResourceType resourceType,
                             ResourceLocation skill, int remainingTicks, int totalCastTicks,
                             boolean canCancelWithMovement) {
        apply(value, policy, resourceType, skill, remainingTicks, totalCastTicks,
                canCancelWithMovement, SkillAimMode.INSTANT_AIM);
    }

    public static void apply(SkillActionState value, MovementPolicy policy, PrimaryResourceType resourceType,
                             ResourceLocation skill, int remainingTicks, int totalCastTicks,
                             boolean canCancelWithMovement, SkillAimMode currentAimMode) {
        action = value;
        movement = policy;
        resource = resourceType;
        activeSkill = skill;
        actionTicks = Math.max(0, remainingTicks);
        castTicks = Math.max(0, totalCastTicks);
        movementCancelAllowed = canCancelWithMovement;
        aimMode = currentAimMode;
    }

    public static SkillActionState action() { return action; }
    public static MovementPolicy movement() { return movement; }
    public static PrimaryResourceType resource() { return resource; }
    public static ResourceLocation activeSkill() { return activeSkill; }
    public static int actionTicks() { return actionTicks; }
    public static int castTicks() { return castTicks; }
    public static boolean movementCancelAllowed() { return movementCancelAllowed; }
    public static SkillAimMode aimMode() { return aimMode; }

    private ClientSkillState() {}
}
