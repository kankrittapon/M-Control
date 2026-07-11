package com.zexqm.rpgproject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class MouseWorldPicker {
    private static final double ENTITY_PICK_DISTANCE = 48.0D;

    public static EntityHitResult pickEntityUnderMouse(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }

        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 direction = ClientAim.cursorRay(minecraft);
        Vec3 end = origin.add(direction.scale(ENTITY_PICK_DISTANCE));
        return ClientAim.pickEntity(minecraft, origin, end);
    }

    public static HitResult pickBlockUnderMouse(Minecraft minecraft) {
        if (minecraft.player == null) {
            return minecraft.hitResult;
        }

        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 end = origin.add(ClientAim.cursorRay(minecraft).scale(ENTITY_PICK_DISTANCE));
        return minecraft.level.clip(new net.minecraft.world.level.ClipContext(
                origin,
                end,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                minecraft.player
        ));
    }

    private MouseWorldPicker() {
    }
}
