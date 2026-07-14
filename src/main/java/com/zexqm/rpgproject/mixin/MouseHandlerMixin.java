package com.zexqm.rpgproject.mixin;

import com.zexqm.rpgproject.client.ClientCameraController;
import com.zexqm.rpgproject.client.ClientControlState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    /**
     * Redirect player.turn() inside turnPlayer().
     * In third-person: send mouse delta to ClientCameraController (camera rotates, not entity).
     * In first-person: let vanilla handle normally.
     */
    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void rpg_redirectTurnToCamera(LocalPlayer player, double yRot, double xRot) {
        if (ClientControlState.isThirdPerson()) {
            if (!ClientControlState.isMouseMovementMode()) {
                ClientCameraController.onMouseInput(yRot, xRot);
            }
        } else {
            player.turn(yRot, xRot);
        }
    }
}
