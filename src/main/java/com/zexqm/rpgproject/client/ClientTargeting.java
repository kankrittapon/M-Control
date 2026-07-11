package com.zexqm.rpgproject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ClientTargeting {
    private static final double MAX_TARGET_DISTANCE = 48.0D;

    private static int targetId = -1;
    private static String targetName = "";

    public static int targetId() {
        return targetId;
    }

    public static String targetName() {
        return targetName;
    }

    public static void setTarget(int id, String name) {
        targetId = id;
        targetName = name;
    }

    public static void clear() {
        targetId = -1;
        targetName = "";
    }

    public static boolean isSelected(Entity entity) {
        return entity != null && entity.getId() == targetId;
    }

    public static void tick(Minecraft minecraft) {
        if (targetId < 0 || minecraft.player == null || minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(targetId);
        if (!(entity instanceof LivingEntity living)
                || !living.isAlive()
                || minecraft.player.distanceTo(living) > MAX_TARGET_DISTANCE) {
            clear();
        }
    }

    private ClientTargeting() {
    }
}
