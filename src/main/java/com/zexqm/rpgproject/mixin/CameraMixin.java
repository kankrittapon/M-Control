package com.zexqm.rpgproject.mixin;

import com.mojang.logging.LogUtils;
import com.zexqm.rpgproject.client.ClientCameraController;
import com.zexqm.rpgproject.client.ClientControlState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Unique private static final Logger RPG_CAMERA_LOGGER = LogUtils.getLogger();
    @Unique private static boolean rpgProject$loggedCameraSetup;
    @Unique private static boolean rpgProject$loggedCameraOverride;

    @Shadow protected abstract void setRotation(float pYRot, float pXRot);
    @Shadow protected abstract void setPosition(double pX, double pY, double pZ);

    @Shadow private Vec3 position;
    @Shadow private BlockGetter level;
    @Shadow private Entity entity;
    @Shadow private Vector3f forwards;
    @Shadow private Vector3f up;
    @Shadow private Vector3f left;

    @Inject(method = "setup", at = @At("TAIL"))
    private void rpg_overrideCameraSetup(
            BlockGetter pLevel, Entity pEntity, boolean pDetached,
            boolean pMirrored, float pPartialTick, CallbackInfo ci) {

        Minecraft minecraft = Minecraft.getInstance();
        if (!rpgProject$loggedCameraSetup) {
            RPG_CAMERA_LOGGER.info(
                    "[RPG Camera] setup detached={} mirrored={} cameraType={} entity={}",
                    pDetached, pMirrored, minecraft.options.getCameraType(), pEntity.getClass().getSimpleName());
            rpgProject$loggedCameraSetup = true;
        }

        boolean renderedThirdPerson = !minecraft.options.getCameraType().isFirstPerson();
        if (!renderedThirdPerson || !(pEntity instanceof LocalPlayer)) {
            return;
        }

        if (!rpgProject$loggedCameraOverride) {
            RPG_CAMERA_LOGGER.info("[RPG Camera] CameraMixin active distance={}", ClientCameraController.getCameraDistance());
            rpgProject$loggedCameraOverride = true;
        }

        float yaw = ClientCameraController.getYaw(pPartialTick) + (pMirrored ? 180.0F : 0.0F);
        float pitch = ClientCameraController.getPitch(pPartialTick);

        // 1. Override rotation (also updates forwards/up/left vectors)
        this.setRotation(yaw, pitch);

        // 2. Set position to entity eye (interpolated)
        double ex = Mth.lerp(pPartialTick, pEntity.xOld, pEntity.getX());
        double ey = Mth.lerp(pPartialTick, pEntity.yOld, pEntity.getY()) + pEntity.getEyeHeight();
        double ez = Mth.lerp(pPartialTick, pEntity.zOld, pEntity.getZ());
        this.setPosition(ex, ey, ez);

        // 3. Calculate safe camera distance (collision check)
        double maxDist = ClientCameraController.getCameraDistance();
        double safeDist = this.rpg_calculateSafeDistance(maxDist);

        // 4. Move camera backward along forward vector
        Vec3 fwd = new Vec3(this.forwards);
        double newX = this.position.x - fwd.x * safeDist;
        double newY = this.position.y - fwd.y * safeDist;
        double newZ = this.position.z - fwd.z * safeDist;

        // 5. Apply shoulder offset
        double sr = ClientCameraController.SHOULDER_OFFSET_RIGHT;
        double su = ClientCameraController.SHOULDER_OFFSET_UP;
        newX += -this.left.x() * sr + this.up.x() * su;
        newY += -this.left.y() * sr + this.up.y() * su;
        newZ += -this.left.z() * sr + this.up.z() * su;

        this.setPosition(newX, newY, newZ);
    }

    /**
     * Reimplementation of Camera#getMaxZoom — 8-corner raycast for wall collision.
     * Returns the maximum safe distance the camera can be placed.
     */
    private double rpg_calculateSafeDistance(double desiredDistance) {
        Vec3 eyePos = this.position;

        for (int i = 0; i < 8; ++i) {
            float offX = ((i & 1) * 2 - 1) * 0.1F;
            float offY = ((i >> 1 & 1) * 2 - 1) * 0.1F;
            float offZ = ((i >> 2 & 1) * 2 - 1) * 0.1F;

            Vec3 start = eyePos.add(offX, offY, offZ);
            Vec3 dir = new Vec3(-this.forwards.x(), -this.forwards.y(), -this.forwards.z());
            Vec3 end = start.add(dir.scale(desiredDistance));

            HitResult hit = this.level.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    this.entity
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                double dist = hit.getLocation().distanceTo(eyePos);
                if (dist < desiredDistance) {
                    desiredDistance = dist;
                }
            }
        }

        return desiredDistance;
    }
}
