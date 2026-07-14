package com.zexqm.rpgproject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Central client-only camera state for BDO-style third-person camera.
 * Stores cameraYaw/cameraPitch INDEPENDENTLY from Entity.rotationYaw/Pitch,
 * allowing the camera to orbit freely without rotating the player entity.
 */
public final class ClientCameraController {

    // ── Current camera angles ────────────────────────────────────────────
    private static float cameraYaw;
    private static float cameraPitch;

    // ── Previous tick values (for lerp interpolation) ────────────────────
    private static float prevCameraYaw;
    private static float prevCameraPitch;

    // ── Target values (for smooth camera) ────────────────────────────────
    private static float targetCameraYaw;
    private static float targetCameraPitch;

    // ── Camera distance behind the player ────────────────────────────────
    private static double cameraDistance = 4.0;

    // ── Initialization guard ─────────────────────────────────────────────
    private static boolean initialized = false;

    // ── Shoulder offset constants ────────────────────────────────────────
    /** Rightward offset for over-the-shoulder framing. */
    public static final float SHOULDER_OFFSET_RIGHT = 0.75F;
    /** Upward offset for over-the-shoulder framing. */
    public static final float SHOULDER_OFFSET_UP = 0.2F;

    // ── Pitch clamp constants ────────────────────────────────────────────
    private static final float MIN_PITCH = -89.0F;
    private static final float MAX_PITCH = 89.0F;

    // ── Distance clamp constants ─────────────────────────────────────────
    private static final double MIN_DISTANCE = 1.0;
    private static final double MAX_DISTANCE = 16.0;

    private ClientCameraController() {
        // Utility class — no instances
    }

    // =====================================================================
    //  Input
    // =====================================================================

    /**
     * Called from {@link com.zexqm.rpgproject.mixin.MouseHandlerMixin}.
     * Values already have sensitivity applied (same as vanilla Entity.turn params).
     * We apply the same 0.15F multiplier that vanilla uses inside Entity#turn.
     *
     * @param yRot horizontal mouse delta (positive = right)
     * @param xRot vertical mouse delta (positive = down)
     */
    public static void onMouseInput(double yRot, double xRot) {
        targetCameraYaw += (float) yRot * 0.15F;
        targetCameraPitch = Mth.clamp(targetCameraPitch + (float) xRot * 0.15F, MIN_PITCH, MAX_PITCH);
    }

    // =====================================================================
    //  Getters — interpolated (for rendering)
    // =====================================================================

    /**
     * @return Interpolated camera yaw for the current partial tick.
     */
    public static float getYaw(float partialTick) {
        return Mth.lerp(partialTick, prevCameraYaw, cameraYaw);
    }

    /**
     * @return Interpolated camera pitch for the current partial tick.
     */
    public static float getPitch(float partialTick) {
        return Mth.lerp(partialTick, prevCameraPitch, cameraPitch);
    }

    // =====================================================================
    //  Getters — raw (for gameplay logic)
    // =====================================================================

    /**
     * @return Raw (non-interpolated) camera yaw.
     */
    public static float getRawYaw() {
        return cameraYaw;
    }

    /**
     * @return Raw (non-interpolated) camera pitch.
     */
    public static float getRawPitch() {
        return cameraPitch;
    }

    /**
     * @return Current camera distance behind the player.
     */
    public static double getCameraDistance() {
        return cameraDistance;
    }

    public static double getMinDistance() {
        return MIN_DISTANCE;
    }

    public static double getMaxDistance() {
        return MAX_DISTANCE;
    }

    public static void setCameraDistance(double distance) {
        cameraDistance = Mth.clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
    }

    // =====================================================================
    //  Distance adjustment (scroll wheel)
    // =====================================================================

    /**
     * Adjusts the camera distance based on scroll input.
     *
     * @param scrollDelta positive = scroll up (closer), negative = scroll down (farther)
     */
    public static void adjustDistance(double scrollDelta) {
        cameraDistance = Mth.clamp(cameraDistance - scrollDelta, MIN_DISTANCE, MAX_DISTANCE);
    }

    // =====================================================================
    //  Tick & Lifecycle
    // =====================================================================

    /**
     * Called every client tick. Stores previous angles for interpolation and
     * performs first-time initialization from the player entity.
     *
     * @param minecraft the Minecraft client instance
     */
    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        if (!initialized) {
            cameraYaw = minecraft.player.getYRot();
            cameraPitch = minecraft.player.getXRot();
            targetCameraYaw = cameraYaw;
            targetCameraPitch = cameraPitch;
            prevCameraYaw = cameraYaw;
            prevCameraPitch = cameraPitch;
            initialized = true;
        }

        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;

        // Smooth camera chase
        cameraYaw = Mth.rotLerp(0.5F, cameraYaw, targetCameraYaw);
        cameraPitch += (targetCameraPitch - cameraPitch) * 0.5F;
    }

    /**
     * Manually initializes camera angles from the given entity.
     *
     * @param entity the entity to read initial rotation from
     */
    public static void initFromEntity(Entity entity) {
        cameraYaw = entity.getYRot();
        cameraPitch = entity.getXRot();
        targetCameraYaw = cameraYaw;
        targetCameraPitch = cameraPitch;
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
        initialized = true;
    }

    /**
     * Resets all camera state. Call when leaving a world / disconnecting.
     */
    public static void reset() {
        initialized = false;
        cameraYaw = 0.0F;
        cameraPitch = 0.0F;
        targetCameraYaw = 0.0F;
        targetCameraPitch = 0.0F;
        prevCameraYaw = 0.0F;
        prevCameraPitch = 0.0F;
        cameraDistance = 4.0;
    }
}
