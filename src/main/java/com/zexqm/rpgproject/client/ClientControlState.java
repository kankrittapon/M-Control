package com.zexqm.rpgproject.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.slf4j.Logger;

/**
 * Central client-side state for RPG view modes and mouse-movement control.
 * Camera style: BDO-like third-person with independent camera rotation.
 *
 * <p>Phase 1: Camera rotation is fully decoupled from entity rotation.
 * Mouse input goes to {@link ClientCameraController}, not to Entity.
 * WASD movement direction follows the camera yaw.</p>
 */
public final class ClientControlState {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MOVE_STOP_DISTANCE = 0.25D;
    private static final double MOVE_TARGET_STOP_DISTANCE = 2.25D;
    private static final double MAX_MOVE_TARGET_DISTANCE = 48.0D;
    private static final float AUTO_MOVE_TURN_SPEED = 8.0F;
    private static final float AUTO_ATTACK_TURN_SPEED = 12.0F;
    private static final double AUTO_ATTACK_CHASE_BUFFER = 0.35D;
    private static final int AUTO_ATTACK_INTERVAL_TICKS = 10;

    public enum MovementState {
        IDLE, MANUAL, NAVIGATING
    }

    private static ClientViewMode viewMode = ClientViewMode.THIRD_PERSON;
    private static boolean mouseMovementMode;
    private static boolean cameraDragMode;
    private static double lastDragMouseX;
    private static double lastDragMouseY;
    private static double dragStartMouseX;
    private static double dragStartMouseY;
    private static boolean cameraDragMoved;
    private static int cameraDragLogCooldown;
    private static Vec3 moveDestination;
    private static int moveTargetId = -1;
    private static int autoAttackTargetId = -1;
    private static boolean autoAttackInRange;
    private static BlockPos autoMinePos;
    private static Direction autoMineDirection;
    private static int autoAttackTicks;
    private static MovementState movementState = MovementState.IDLE;

    // ── Getters ──────────────────────────────────────────────────────────

    public static ClientViewMode viewMode() {
        return viewMode;
    }

    public static boolean isThirdPerson() {
        return viewMode == ClientViewMode.THIRD_PERSON;
    }

    public static boolean isMouseMovementMode() {
        return mouseMovementMode;
    }

    public static boolean isCameraDragMode() {
        return cameraDragMode;
    }

    public static boolean isAutoAttacking() {
        return autoAttackTargetId >= 0 || autoMinePos != null;
    }

    public static boolean isAutoMoving() {
        return movementState == MovementState.NAVIGATING;
    }

    public static MovementState getMovementState() {
        return movementState;
    }

    // ── View Mode Toggle ─────────────────────────────────────────────────

    public static void toggleViewMode(Minecraft minecraft) {
        if (viewMode == ClientViewMode.FIRST_PERSON) {
            viewMode = ClientViewMode.THIRD_PERSON;
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);

            // Initialize camera angles from entity when entering third-person
            if (minecraft.player != null) {
                ClientCameraController.initFromEntity(minecraft.player);
            }
            showMessage(minecraft, Component.translatable("message.rpg_project.view.third_person"));
            return;
        }

        // Switching to first-person: sync entity rotation from camera
        if (minecraft.player != null) {
            minecraft.player.setYRot(ClientCameraController.getRawYaw());
            minecraft.player.setXRot(ClientCameraController.getRawPitch());
            minecraft.player.setYHeadRot(ClientCameraController.getRawYaw());
        }

        viewMode = ClientViewMode.FIRST_PERSON;
        exitMouseMovementMode(minecraft);
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        grabMouse(minecraft);
        showMessage(minecraft, Component.translatable("message.rpg_project.view.first_person"));
    }

    // ── Mouse Movement Mode (Ctrl-hold) ──────────────────────────────────

    /**
     * Called every client tick with the current Ctrl-held state.
     * Entering the mode releases the mouse cursor so the player can click
     * on blocks / entities in the world. Leaving re-grabs it.
     */
    public static void updateMouseMovementMode(Minecraft minecraft, boolean ctrlHeld) {
        boolean next = isThirdPerson() && ctrlHeld && minecraft.screen == null;

        if (next && !mouseMovementMode) {
            // ▶ Entering mouse movement mode — show cursor
            releaseMouse(minecraft);
        } else if (!next && mouseMovementMode) {
            // ◀ Leaving mouse movement mode — hide cursor
            cameraDragMode = false;
            grabMouse(minecraft);
        }

        mouseMovementMode = next;
    }

    public static void setCameraDragMode(boolean dragging) {
        boolean wasDragging = cameraDragMode;
        cameraDragMode = dragging && mouseMovementMode;
        if (cameraDragMode) {
            Minecraft minecraft = Minecraft.getInstance();
            dragStartMouseX = lastDragMouseX = minecraft.mouseHandler.xpos();
            dragStartMouseY = lastDragMouseY = minecraft.mouseHandler.ypos();
            cameraDragMoved = false;
            cameraDragLogCooldown = 0;
            if (!wasDragging) {
                LOGGER.info("[RPG Input] camera drag start mouse=({}, {})", lastDragMouseX, lastDragMouseY);
            }
        } else if (wasDragging) {
            LOGGER.info("[RPG Input] camera drag stop moved={}", cameraDragMoved);
        }
    }

    public static boolean consumeCameraDragMoved() {
        boolean moved = cameraDragMoved;
        cameraDragMoved = false;
        return moved;
    }

    // ── Click-to-move ────────────────────────────────────────────────────

    public static void setMoveDestination(Vec3 destination) {
        moveDestination = destination;
        moveTargetId = -1;
        movementState = MovementState.NAVIGATING;
    }

    public static void setMoveTarget(int entityId) {
        moveDestination = null;
        moveTargetId = entityId;
        movementState = MovementState.NAVIGATING;
    }

    public static void startAutoAttackTarget(int entityId) {
        stopAutoAttack();
        autoAttackTargetId = entityId;
        autoAttackInRange = false;
        setMoveTarget(entityId);
    }

    public static void startAutoMineBlock(BlockPos pos, Direction direction) {
        stopAutoAttack();
        autoMinePos = pos.immutable();
        autoMineDirection = direction;
    }

    public static void stopAutoAttack() {
        autoAttackTargetId = -1;
        autoAttackInRange = false;
        autoMinePos = null;
        autoMineDirection = null;
        autoAttackTicks = 0;
    }

    public static void cancelAutoMove() {
        moveDestination = null;
        moveTargetId = -1;
        if (movementState == MovementState.NAVIGATING) {
            movementState = MovementState.IDLE;
        }
    }

    // ── Tick ─────────────────────────────────────────────────────────────

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        if (shouldUseVanillaMovement(minecraft)) {
            cancelAutoMove();
            stopAutoAttack();
            return;
        }

        // Force camera back if something (F5) switched it to first-person
        if (isThirdPerson() && minecraft.options.getCameraType().isFirstPerson()) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        // ── State Machine Update ──────────────────────────────────────
        if (isManualMovementPressed(minecraft)) {
            cancelAutoMove();
            stopAutoAttack();
            movementState = MovementState.MANUAL;
        } else if (movementState == MovementState.MANUAL) {
            movementState = MovementState.IDLE;
        }

        tickCameraDrag(minecraft);
        tickAutoAttack(minecraft);

        // ── Auto-move (click-to-move) ─────────────────────────────────
        if (movementState != MovementState.NAVIGATING || minecraft.player.input == null) {
            return;
        }

        Vec3 destination = resolveMoveDestination(minecraft);
        if (destination == null) {
            cancelAutoMove();
            return;
        }

        double stopDistance = moveTargetId >= 0 ? MOVE_TARGET_STOP_DISTANCE : MOVE_STOP_DISTANCE;
        Vec3 delta = destination.subtract(minecraft.player.position());
        Vec3 flat = new Vec3(delta.x, 0.0D, delta.z);
        if (flat.lengthSqr() <= stopDistance * stopDistance) {
            cancelAutoMove();
            return;
        }

        // Calculate yaw toward destination (Minecraft convention: 0=South, +90=West)
        float targetYaw = (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
        float currentYaw = minecraft.player.getYRot();
        float smoothedYaw = Mth.approachDegrees(currentYaw, targetYaw, AUTO_MOVE_TURN_SPEED);
        minecraft.player.setYRot(smoothedYaw);
        minecraft.player.setYHeadRot(smoothedYaw);
        minecraft.player.setSprinting(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void exitMouseMovementMode(Minecraft minecraft) {
        mouseMovementMode = false;
        cameraDragMode = false;
        cancelAutoMove();
    }

    private static boolean isManualMovementPressed(Minecraft minecraft) {
        return minecraft.options.keyUp.isDown()
                || minecraft.options.keyDown.isDown()
                || minecraft.options.keyLeft.isDown()
                || minecraft.options.keyRight.isDown()
                || minecraft.options.keyJump.isDown();
    }

    private static Vec3 resolveMoveDestination(Minecraft minecraft) {
        if (moveTargetId < 0) {
            return moveDestination;
        }

        Entity entity = minecraft.level.getEntity(moveTargetId);
        if (!(entity instanceof LivingEntity living)
                || !living.isAlive()
                || minecraft.player.distanceTo(living) > MAX_MOVE_TARGET_DISTANCE) {
            return null;
        }

        return living.position();
    }

    private static boolean shouldUseVanillaMovement(Minecraft minecraft) {
        return minecraft.player.getAbilities().flying
                || minecraft.player.isInWaterOrBubble()
                || minecraft.player.isInLava();
    }

    private static void tickCameraDrag(Minecraft minecraft) {
        if (!cameraDragMode) {
            return;
        }

        if (!mouseMovementMode) {
            LOGGER.info("[RPG Input] camera drag canceled because mouse movement mode ended");
            cameraDragMode = false;
            return;
        }

        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        double deltaX = mouseX - lastDragMouseX;
        double deltaY = mouseY - lastDragMouseY;
        lastDragMouseX = mouseX;
        lastDragMouseY = mouseY;

        if (deltaX != 0.0D || deltaY != 0.0D) {
            double totalX = mouseX - dragStartMouseX;
            double totalY = mouseY - dragStartMouseY;
            if (totalX * totalX + totalY * totalY >= 16.0D) {
                cameraDragMoved = true;
            }
            ClientCameraController.onCursorDrag(deltaX, deltaY);
            if (cameraDragLogCooldown-- <= 0) {
                LOGGER.info("[RPG Input] camera drag delta=({}, {}) total=({}, {})", deltaX, deltaY, totalX, totalY);
                cameraDragLogCooldown = 12;
            }
        }
    }

    private static void tickAutoAttack(Minecraft minecraft) {
        if (minecraft.gameMode == null) {
            stopAutoAttack();
            return;
        }

        if (autoMinePos != null) {
            if (minecraft.level.getBlockState(autoMinePos).isAir()) {
                minecraft.gameMode.stopDestroyBlock();
                stopAutoAttack();
                return;
            }

            minecraft.gameMode.continueDestroyBlock(autoMinePos, autoMineDirection == null ? Direction.UP : autoMineDirection);
            return;
        }

        if (autoAttackTargetId < 0) {
            return;
        }

        Entity entity = minecraft.level.getEntity(autoAttackTargetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            stopAutoAttack();
            cancelAutoMove();
            return;
        }

        double distance = distanceToHitbox(minecraft.player.getEyePosition(), living.getBoundingBox());
        double attackReach = minecraft.player.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        faceEntitySmoothly(minecraft, living);

        if (autoAttackInRange && distance > attackReach + AUTO_ATTACK_CHASE_BUFFER) {
            autoAttackInRange = false;
        } else if (!autoAttackInRange && distance <= attackReach) {
            autoAttackInRange = true;
        }

        if (!autoAttackInRange) {
            setMoveTarget(autoAttackTargetId);
            return;
        }

        autoAttackTicks--;
        if (autoAttackTicks > 0) {
            return;
        }

        minecraft.gameMode.attack(minecraft.player, living);
        minecraft.player.swing(InteractionHand.MAIN_HAND);
        autoAttackTicks = AUTO_ATTACK_INTERVAL_TICKS;
    }

    private static void faceEntitySmoothly(Minecraft minecraft, LivingEntity target) {
        Vec3 flat = target.position().subtract(minecraft.player.position());
        if (flat.horizontalDistanceSqr() <= 1.0E-6D) {
            return;
        }

        float targetYaw = (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
        float bodyYaw = Mth.approachDegrees(minecraft.player.getYRot(), targetYaw, AUTO_ATTACK_TURN_SPEED);
        minecraft.player.setYRot(bodyYaw);
        minecraft.player.setYHeadRot(bodyYaw);
    }

    private static double distanceToHitbox(Vec3 origin, AABB box) {
        double x = Mth.clamp(origin.x, box.minX, box.maxX);
        double y = Mth.clamp(origin.y, box.minY, box.maxY);
        double z = Mth.clamp(origin.z, box.minZ, box.maxZ);
        return origin.distanceTo(new Vec3(x, y, z));
    }

    private static void grabMouse(Minecraft minecraft) {
        if (minecraft.screen == null && !minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.grabMouse();
        }
    }

    private static void releaseMouse(Minecraft minecraft) {
        if (minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
    }

    private static void showMessage(Minecraft minecraft, Component message) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    private ClientControlState() {
    }
}
