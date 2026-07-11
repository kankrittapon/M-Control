package com.zexqm.rpgproject.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ClientAim {
    private static final double AIM_DISTANCE = 48.0D;

    public record AimResult(Vec3 origin, Vec3 direction, EntityHitResult entityHit, HitResult blockHit) {
        public Vec3 targetPoint() {
            if (entityHit != null) {
                return entityHit.getLocation();
            }
            if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
                return blockHit.getLocation();
            }
            return origin.add(direction.scale(AIM_DISTANCE));
        }
    }

    public static AimResult current(Minecraft minecraft) {
        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 direction = ClientControlState.isMouseMovementMode() ? cursorRay(minecraft) : cameraRay(minecraft);
        return fromRay(minecraft, origin, direction);
    }

    public static AimResult fromCursor(Minecraft minecraft) {
        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        return fromRay(minecraft, origin, cursorRay(minecraft));
    }

    public static AimResult fromCamera(Minecraft minecraft) {
        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        return fromRay(minecraft, origin, cameraRay(minecraft));
    }

    public static AimResult fromRay(Minecraft minecraft, Vec3 origin, Vec3 direction) {
        if (minecraft.level == null || minecraft.player == null) {
            return new AimResult(origin, direction.normalize(), null, null);
        }

        Vec3 normalized = direction.normalize();
        Vec3 end = origin.add(normalized.scale(AIM_DISTANCE));
        EntityHitResult entityHit = pickEntity(minecraft, origin, end);
        HitResult blockHit = minecraft.level.clip(new ClipContext(
                origin,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));

        if (entityHit != null && blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            double entityDistance = origin.distanceToSqr(entityHit.getLocation());
            double blockDistance = origin.distanceToSqr(blockHit.getLocation());
            if (blockDistance < entityDistance) {
                entityHit = null;
            }
        }

        return new AimResult(origin, normalized, entityHit, blockHit);
    }

    public static EntityHitResult pickEntity(Minecraft minecraft, Vec3 origin, Vec3 end) {
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }

        AABB searchBox = new AABB(origin, end).inflate(2.0D);
        Entity bestEntity = null;
        Vec3 bestHit = null;
        double bestDistance = AIM_DISTANCE * AIM_DISTANCE;
        Player player = minecraft.player;

        for (Entity entity : minecraft.level.getEntities(player, searchBox, entity ->
                entity instanceof LivingEntity living && living.isAlive() && entity != player && !entity.isSpectator())) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.35D);
            Vec3 hit = box.clip(origin, end).orElse(null);
            if (hit == null) {
                continue;
            }

            double distance = origin.distanceToSqr(hit);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestEntity = entity;
                bestHit = hit;
            }
        }

        return bestEntity == null ? null : new EntityHitResult(bestEntity, bestHit);
    }

    public static Vec3 cameraRay(Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        return Vec3.directionFromRotation(camera.getXRot(), camera.getYRot()).normalize();
    }

    public static Vec3 cursorRay(Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        double width = minecraft.getWindow().getScreenWidth();
        double height = minecraft.getWindow().getScreenHeight();
        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();

        double ndcX = (mouseX / width) * 2.0D - 1.0D;
        double ndcY = 1.0D - (mouseY / height) * 2.0D;
        double fov = Math.toRadians(minecraft.options.fov().get());
        double aspect = width / height;
        double tan = Math.tan(fov / 2.0D);

        Vec3 forward = cameraRay(minecraft);
        Vec3 right = Vec3.directionFromRotation(0.0F, camera.getYRot() + 90.0F).normalize();
        Vec3 up = right.cross(forward).normalize();
        return forward.add(right.scale(ndcX * aspect * tan)).add(up.scale(ndcY * tan)).normalize();
    }

    private ClientAim() {
    }
}
