package com.zexqm.rpgproject.client;

import com.zexqm.rpgproject.rpg.skill.MovementPolicy;
import com.zexqm.rpgproject.rpg.skill.PrimaryResourceType;
import com.zexqm.rpgproject.rpg.skill.SkillActionState;
import net.minecraft.resources.ResourceLocation;

public final class ClientSkillState {
    private static SkillActionState action = SkillActionState.SHEATHED;
    private static MovementPolicy movement = MovementPolicy.FULL;
    private static PrimaryResourceType resource = PrimaryResourceType.MP;
    private static ResourceLocation activeSkill;
    private static int actionTicks;
    private static int castTicks;
    private static boolean movementCancelAllowed;

    public static void apply(SkillActionState value, MovementPolicy policy, PrimaryResourceType resourceType,
                             ResourceLocation skill, int remainingTicks, int totalCastTicks) {
        apply(value, policy, resourceType, skill, remainingTicks, totalCastTicks, false);
    }

    public static void apply(SkillActionState value, MovementPolicy policy, PrimaryResourceType resourceType,
                             ResourceLocation skill, int remainingTicks, int totalCastTicks,
                             boolean canCancelWithMovement) {
        action = value;
        movement = policy;
        resource = resourceType;
        activeSkill = skill;
        actionTicks = Math.max(0, remainingTicks);
        castTicks = Math.max(0, totalCastTicks);
        movementCancelAllowed = canCancelWithMovement;
    }

    public static SkillActionState action() { return action; }
    public static MovementPolicy movement() { return movement; }
    public static PrimaryResourceType resource() { return resource; }
    public static ResourceLocation activeSkill() { return activeSkill; }
    public static int actionTicks() { return actionTicks; }
    public static int castTicks() { return castTicks; }
    public static boolean movementCancelAllowed() { return movementCancelAllowed; }

    private ClientSkillState() {}
}
