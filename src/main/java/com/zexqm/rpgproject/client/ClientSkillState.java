package com.zexqm.rpgproject.client;

import com.zexqm.rpgproject.rpg.skill.MovementPolicy;
import com.zexqm.rpgproject.rpg.skill.PrimaryResourceType;
import com.zexqm.rpgproject.rpg.skill.SkillActionState;

public final class ClientSkillState {
    private static SkillActionState action = SkillActionState.SHEATHED;
    private static MovementPolicy movement = MovementPolicy.FULL;
    private static PrimaryResourceType resource = PrimaryResourceType.MP;

    public static void apply(SkillActionState value, MovementPolicy policy, PrimaryResourceType resourceType) {
        action = value;
        movement = policy;
        resource = resourceType;
    }

    public static SkillActionState action() { return action; }
    public static MovementPolicy movement() { return movement; }
    public static PrimaryResourceType resource() { return resource; }

    private ClientSkillState() {}
}
