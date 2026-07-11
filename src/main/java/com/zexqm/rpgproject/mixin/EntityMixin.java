package com.zexqm.rpgproject.mixin;

import com.zexqm.rpgproject.client.ClientTargeting;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    /**
     * Makes the entity render with a glowing outline on the client 
     * if it is the currently selected target.
     */
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void rpg_forceGlowIfTargeted(CallbackInfoReturnable<Boolean> cir) {
        if (ClientTargeting.isSelected((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
