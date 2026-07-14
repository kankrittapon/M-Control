package com.zexqm.rpgproject.mixin;

import com.zexqm.rpgproject.client.ClientAim;
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
 * <p>Manual WASD retains vanilla forward/back/strafe movement. Click-to-move
 * owns its automatic yaw separately in {@link ClientControlState}.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    private static final float MANUAL_AIM_TURN_SPEED = 30.0F;

    @Inject(method = "tick", at = @At("HEAD"))
    private void rpg_faceManualAim(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer player) || !ClientControlState.isThirdPerson()
                || ClientControlState.isAutoMoving()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player || mc.level == null || mc.screen != null) return;
        int forwardAxis = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        int strafeAxis = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        if (forwardAxis == 0 && strafeAxis == 0) return;

        var aim = ClientAim.current(mc);
        var direction = aim.targetPoint().subtract(player.getEyePosition());
        if (direction.horizontalDistanceSqr() < 1.0E-6D) return;

        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float yaw = Mth.approachDegrees(player.getYRot(), targetYaw, MANUAL_AIM_TURN_SPEED);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
    }

    /**
     * At TAIL of tick: override vanilla's yBodyRot calculation.
     * Vanilla would snap or drift body rotation using its own logic;
     * we replace it with a smooth lerp toward our entity yaw.
     *
     * <p>This does not change entity yaw from manual movement input.
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
}
