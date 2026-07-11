package com.zexqm.rpgproject.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zexqm.rpgproject.RpgProject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID, value = Dist.CLIENT)
public final class MobOverlayRenderer {
    private static final double MAX_RENDER_DISTANCE = 48.0D;

    @SubscribeEvent
    public static void renderLivingOverlay(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || entity == player || entity.isInvisibleTo(player) || player.distanceTo(entity) > MAX_RENDER_DISTANCE) {
            return;
        }

        renderOverlay(entity, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    private static void renderOverlay(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        Font font = minecraft.font;
        boolean selected = ClientTargeting.isSelected(entity);

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.65D, 0.0D);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        Component name = entity.getDisplayName();
        float nameX = -font.width(name) / 2.0F;
        font.drawInBatch(name, nameX, -18.0F, selected ? 0xFF66FFFF : 0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0x66000000, packedLight);

        float healthPercent = Math.max(0.0F, Math.min(1.0F, entity.getHealth() / entity.getMaxHealth()));
        int fillColor = selected ? 0xFF37E6FF : 0xFFE64646;
        String background = "----------";
        String foreground = "|".repeat(Math.max(0, Math.round(background.length() * healthPercent)));
        float barX = -font.width(background) / 2.0F;
        font.drawInBatch(background, barX, -6.0F, 0xFF2A1010, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0x66000000, packedLight);
        font.drawInBatch(foreground, barX, -6.0F, fillColor, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
        poseStack.popPose();
    }

    private MobOverlayRenderer() {
    }
}
