package com.zexqm.rpgproject.mixin;

import com.zexqm.rpgproject.client.ClientCameraController;
import com.zexqm.rpgproject.client.ClientControlState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides vanilla body rotation for the local player in third-person mode.
 *
 * <p>Phase 2 — Body Yaw follows Movement Direction:
 * <ul>
 *   <li>HEAD: Set entity yaw based on WASD + camera direction (before vanilla processes movement)</li>
 *   <li>TAIL: Override yBodyRot to lerp smoothly (after vanilla's body rotation logic)</li>
 * </ul>
 *
 * <p>WASD offsets relative to camera yaw:
 * W=0°, A=−90°, S=±180°, D=+90°, diagonals interpolated (W+D=+45°, etc.)
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * At HEAD of tick: calculate and apply entity yaw from WASD keys + camera direction.
     * Runs BEFORE vanilla processes body rotation and movement, ensuring
     * travel() uses our yaw for movement direction this tick.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void rpg_updateMovementYaw(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer player)) return;
        if (!ClientControlState.isThirdPerson()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player || mc.level == null) return;

        // Click-to-move handles its own yaw in ClientControlState
        if (ClientControlState.isAutoMoving()) return;

        boolean w = mc.options.keyUp.isDown();
        boolean s = mc.options.keyDown.isDown();
        boolean a = mc.options.keyLeft.isDown();
        boolean d = mc.options.keyRight.isDown();

        if (!w && !s && !a && !d) return;

        float cameraYaw = ClientCameraController.getRawYaw();
        float offset = rpg_movementOffset(w, s, a, d);
        float targetYaw = cameraYaw + offset;

        // Smooth lerp toward target (15°/tick — not instant snap)
        float smoothed = Mth.approachDegrees(player.getYRot(), targetYaw, 15.0F);
        player.setYRot(smoothed);
        player.setYHeadRot(smoothed);
    }

    /**
     * At TAIL of tick: override vanilla's yBodyRot calculation.
     * Vanilla would snap or drift body rotation using its own logic;
     * we replace it with a smooth lerp toward our entity yaw.
     *
     * <p>When no WASD is pressed and not auto-moving, yRot doesn't change
     * (MouseHandlerMixin prevents mouse from writing to entity rotation),
     * so yBodyRot naturally stays at the last movement direction.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void rpg_overrideBodyRotation(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer player)) return;
        if (!ClientControlState.isThirdPerson()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return;

        // Lerp body rotation toward entity yaw (20°/tick for smooth animation)
        self.yBodyRot = Mth.approachDegrees(self.yBodyRot, player.getYRot(), 20.0F);
    }

    /**
     * Calculate yaw offset based on WASD key combination.
     * Uses atan2 for proper diagonal support.
     *
     * @return offset in degrees: W=0, A=−90, S=±180, D=+90, W+A=−45, W+D=+45, etc.
     */
    private static float rpg_movementOffset(boolean w, boolean s, boolean a, boolean d) {
        float fwd = 0, strafe = 0;
        if (w) fwd += 1;
        if (s) fwd -= 1;
        if (a) strafe += 1;   // left = positive strafe
        if (d) strafe -= 1;   // right = negative strafe
        if (fwd == 0 && strafe == 0) return 0;

        // atan2(strafe, forward) → angle from forward direction
        // Negate: left(A)=positive strafe should give negative yaw offset (turn left)
        return -(float) Math.toDegrees(Math.atan2(strafe, fwd));
    }
}
