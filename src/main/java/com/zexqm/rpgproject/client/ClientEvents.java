package com.zexqm.rpgproject.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.ClientMana;
import com.zexqm.rpgproject.network.CastMagicBoltPacket;
import com.zexqm.rpgproject.network.RpgNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final long DOUBLE_CLICK_MS = 350L;
    private static long lastLeftClickTime;
    private static int lastLeftClickEntityId = -1;
    private static BlockPos lastLeftClickBlockPos;

    public static final KeyMapping TOGGLE_VIEW_MODE = new KeyMapping(
            "key.rpg_project.toggle_view_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.rpg_project"
    );
    public static final KeyMapping TOGGLE_MOUSE_MOVEMENT = new KeyMapping(
            "key.rpg_project.toggle_mouse_movement",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.categories.rpg_project"
    );
    public static final KeyMapping CAST_MAGIC_BOLT = new KeyMapping(
            "key.rpg_project.cast_magic_bolt",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.rpg_project"
    );

    @Mod.EventBusSubscriber(modid = RpgProject.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_VIEW_MODE);
            event.register(TOGGLE_MOUSE_MOVEMENT);
            event.register(CAST_MAGIC_BOLT);
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        while (TOGGLE_VIEW_MODE.consumeClick()) {
            ClientControlState.toggleViewMode(minecraft);
        }

        ClientCameraController.tick(minecraft);
        ClientControlState.updateMouseMovementMode(minecraft, TOGGLE_MOUSE_MOVEMENT.isDown());
        // Phase 5: Auto-mantle (Set step height to 1.25 blocks so player steps up automatically)
        if (minecraft.player != null) {
            minecraft.player.setMaxUpStep(1.25F);
        }

        ClientControlState.tick(minecraft);
        ClientTargeting.tick(minecraft);

        while (CAST_MAGIC_BOLT.consumeClick()) {
            ClientAim.AimResult aim = ClientAim.current(minecraft);
            Vec3 direction = aim.targetPoint().subtract(minecraft.player.getEyePosition()).normalize();
            facePlayerToward(minecraft, direction);
            RpgNetwork.CHANNEL.sendToServer(new CastMagicBoltPacket(direction));
        }

        while (!ClientControlState.isMouseMovementMode() && minecraft.options.keyAttack.consumeClick()) {
            selectTarget(minecraft);
        }
    }

    @SubscribeEvent
    public static void addCameraOptionsButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof OptionsScreen optionsScreen)) return;

        event.addListener(Button.builder(
                        Component.translatable("screen.rpg_project.camera.open"),
                        button -> Minecraft.getInstance().setScreen(new CameraSettingsScreen(optionsScreen)))
                .bounds(optionsScreen.width - 112, 6, 106, 20)
                .build());
    }

    @SubscribeEvent
    public static void mouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || !ClientControlState.isMouseMovementMode()) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_RELEASE) {
            ClientControlState.setCameraDragMode(false);
            if (!ClientControlState.consumeCameraDragMoved()) {
                handleLeftClick(minecraft);
            }
            event.setCanceled(true);
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            ClientControlState.setCameraDragMode(true);
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            handleRightClick(minecraft);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && ClientControlState.isThirdPerson() && shouldZoomCamera(minecraft)) {
            ClientCameraController.adjustDistance(event.getScrollDelta());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (ClientControlState.isAutoMoving()) {
            event.getInput().up = true;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
            event.getInput().forwardImpulse = 1.0F;
            event.getInput().leftImpulse = 0.0F;
        }
    }

    @SubscribeEvent
    public static void renderPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            if (ClientControlState.isThirdPerson()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int barWidth = 96;
        int x = width / 2 - 91;
        int y = height - 59;

        drawBar(graphics, x, y, barWidth, 8, player.getHealth() / player.getMaxHealth(), 0xFF5C1515, 0xFFE04242);
        drawBar(graphics, x, y + 11, barWidth, 8, (float) ClientMana.mana() / ClientMana.maxMana(), 0xFF102348, 0xFF3390FF);
        graphics.drawString(minecraft.font, "HP " + Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()), x + 2, y - 9, 0xFFFFFFFF, true);
        graphics.drawString(minecraft.font, "MP " + ClientMana.mana() + "/" + ClientMana.maxMana(), x + 2, y + 21, 0xFFFFFFFF, true);

        // Dynamic Crosshair / Target Info
        if (ClientControlState.isThirdPerson()) {
            if (!ClientControlState.isMouseMovementMode()) {
                // Action Mode: Center crosshair
                EntityHitResult ehr = ClientAim.fromCamera(minecraft).entityHit();
                Entity hoveredEntity = ehr == null ? null : ehr.getEntity();

                if (hoveredEntity instanceof LivingEntity living && living.isAlive()) {
                    // Hostile/Active crosshair
                    graphics.fill(width / 2 - 4, height / 2 - 1, width / 2 + 4, height / 2 + 1, 0xFFFF3333);
                    graphics.fill(width / 2 - 1, height / 2 - 4, width / 2 + 1, height / 2 + 4, 0xFFFF3333);
                    
                    int hpBarWidth = 40;
                    float hpPercent = living.getHealth() / living.getMaxHealth();
                    drawBar(graphics, width / 2 - hpBarWidth / 2, height / 2 + 10, hpBarWidth, 4, hpPercent, 0xAA000000, 0xFFFF3333);
                    graphics.drawCenteredString(minecraft.font, living.getDisplayName(), width / 2, height / 2 + 16, 0xFFFFFFFF);
                } else {
                    // Normal crosshair
                    graphics.fill(width / 2 - 1, height / 2 - 1, width / 2 + 1, height / 2 + 1, 0xAAFFFFFF);
                }
            } else {
                // Mouse Mode: HUD indicator and floating info at cursor
                graphics.drawCenteredString(minecraft.font, Component.translatable("message.rpg_project.mouse_mode.hud"), width / 2, height / 2 + 40, 0xFFE6F7FF);
                
                EntityHitResult ehr = ClientAim.fromCursor(minecraft).entityHit();
                if (ehr != null && ehr.getEntity() instanceof LivingEntity living && living.isAlive()) {
                    double mouseX = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
                    double mouseY = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
                    int mx = (int) mouseX;
                    int my = (int) mouseY;

                    int hpBarWidth = 40;
                    float hpPercent = living.getHealth() / living.getMaxHealth();
                    drawBar(graphics, mx - hpBarWidth / 2, my + 15, hpBarWidth, 4, hpPercent, 0xAA000000, 0xFFFF3333);
                    graphics.drawCenteredString(minecraft.font, living.getDisplayName(), mx, my + 21, 0xFFFFFFFF);
                }
            }
            
            // Draw selected target info globally if we have one and we aren't already hovering it
            if (ClientTargeting.targetId() >= 0) {
                Entity targetEntity = minecraft.level.getEntity(ClientTargeting.targetId());
                boolean isHoveringTarget = false;
                
                EntityHitResult hovered = ClientAim.current(minecraft).entityHit();
                if (hovered != null && hovered.getEntity() == targetEntity) {
                    isHoveringTarget = true;
                }
                
                if (!isHoveringTarget && targetEntity instanceof LivingEntity living && living.isAlive()) {
                    Component targetText = Component.literal("Target: ").append(living.getDisplayName()).withStyle(ChatFormatting.AQUA);
                    graphics.drawCenteredString(minecraft.font, targetText, width / 2, 20, 0xFFFFFFFF);
                    
                    int hpBarWidth = 100;
                    float hpPercent = living.getHealth() / living.getMaxHealth();
                    drawBar(graphics, width / 2 - hpBarWidth / 2, 32, hpBarWidth, 6, hpPercent, 0xAA000000, 0xFFFF3333);
                }
            }
        }
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, float fill, int backgroundColor, int fillColor) {
        int clampedFill = Math.max(0, Math.min(width, Math.round(width * fill)));
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xCC000000);
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        graphics.fill(x, y, x + clampedFill, y + height, fillColor);
    }

    private static void selectTarget(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        EntityHitResult entityHit = ClientAim.fromCamera(minecraft).entityHit();
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living && living.isAlive() && living != minecraft.player) {
            Entity entity = entityHit.getEntity();
            ClientTargeting.setTarget(entity.getId(), entity.getDisplayName().getString());
            minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.target.selected", entity.getDisplayName()), true);
            return;
        }

        ClientTargeting.clear();
        minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.target.cleared"), true);
    }

    private static void handleLeftClick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        long now = System.currentTimeMillis();
        EntityHitResult ehr = ClientAim.fromCursor(minecraft).entityHit();
        if (ehr != null && ehr.getEntity() instanceof LivingEntity entity && entity != minecraft.player) {
            if (lastLeftClickEntityId == entity.getId() && now - lastLeftClickTime <= DOUBLE_CLICK_MS) {
                ClientControlState.startAutoAttackTarget(entity.getId());
                minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.attack.target", entity.getDisplayName()), true);
                resetDoubleClick();
                return;
            }

            rememberEntityClick(entity.getId(), now);
            ClientTargeting.setTarget(entity.getId(), entity.getDisplayName().getString());
            minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.target.selected", entity.getDisplayName()), true);
            return;
        }

        HitResult blockHit = ClientAim.fromCursor(minecraft).blockHit();
        if (blockHit instanceof BlockHitResult bhr && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            if (pos.equals(lastLeftClickBlockPos) && now - lastLeftClickTime <= DOUBLE_CLICK_MS) {
                ClientControlState.startAutoMineBlock(pos, bhr.getDirection());
                minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.attack.block"), true);
                resetDoubleClick();
                return;
            }

            rememberBlockClick(pos, now);
        } else {
            resetDoubleClick();
        }

        ClientTargeting.clear();
        minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.target.cleared"), true);
    }

    /** Right click issues movement commands only: entity follow or ground move. */
    private static void handleRightClick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        EntityHitResult ehr = ClientAim.fromCursor(minecraft).entityHit();
        if (ehr != null && ehr.getEntity() instanceof LivingEntity entity && entity != minecraft.player) {
            ClientControlState.setMoveTarget(entity.getId());
            minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.move.target", entity.getDisplayName()), true);
            return;
        }

        HitResult blockHit = ClientAim.fromCursor(minecraft).blockHit();
        if (blockHit instanceof BlockHitResult bhr && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos().relative(bhr.getDirection());
            Vec3 dest = pos.getCenter();
            Vec3 playerPos = minecraft.player.position();
            
            // Distance clamp
            double maxDist = 24.0D;
            if (playerPos.distanceToSqr(dest) > maxDist * maxDist) {
                Vec3 dir = dest.subtract(playerPos).normalize();
                dest = playerPos.add(dir.scale(maxDist));
            }
            
            // LOS check (eye to eye-level at destination)
            Vec3 eyePos = minecraft.player.getEyePosition();
            Vec3 destEye = new Vec3(dest.x, dest.y + minecraft.player.getEyeHeight(), dest.z);
            HitResult losHit = minecraft.level.clip(new net.minecraft.world.level.ClipContext(
                    eyePos, destEye, 
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, 
                    net.minecraft.world.level.ClipContext.Fluid.NONE, 
                    minecraft.player));
            
            if (losHit.getType() == HitResult.Type.BLOCK) {
                Vec3 dir = destEye.subtract(eyePos).normalize();
                // Move back slightly from the wall
                Vec3 hitLoc = losHit.getLocation().subtract(dir.scale(0.5D));
                dest = new Vec3(hitLoc.x, dest.y, hitLoc.z);
            }

            spawnGroundClickMarker(minecraft, bhr.getLocation());
            ClientControlState.setMoveDestination(dest);
            minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.move.destination"), true);
        }
    }

    private static boolean shouldZoomCamera(Minecraft minecraft) {
        return ClientControlState.isMouseMovementMode() || isShiftDown(minecraft);
    }

    private static boolean isShiftDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static void spawnGroundClickMarker(Minecraft minecraft, Vec3 location) {
        if (minecraft.level == null) {
            return;
        }

        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2.0D / 12.0D) * i;
            double x = Math.cos(angle) * 0.35D;
            double z = Math.sin(angle) * 0.35D;
            minecraft.level.addParticle(
                    ParticleTypes.END_ROD,
                    location.x + x,
                    location.y + 0.05D,
                    location.z + z,
                    x * 0.02D,
                    0.015D,
                    z * 0.02D
            );
        }
        minecraft.level.addParticle(ParticleTypes.HAPPY_VILLAGER, location.x, location.y + 0.15D, location.z, 0.0D, 0.04D, 0.0D);
    }

    private static void rememberEntityClick(int entityId, long time) {
        lastLeftClickEntityId = entityId;
        lastLeftClickBlockPos = null;
        lastLeftClickTime = time;
    }

    private static void rememberBlockClick(BlockPos pos, long time) {
        lastLeftClickEntityId = -1;
        lastLeftClickBlockPos = pos.immutable();
        lastLeftClickTime = time;
    }

    private static void resetDoubleClick() {
        lastLeftClickEntityId = -1;
        lastLeftClickBlockPos = null;
        lastLeftClickTime = 0L;
    }

    private static void facePlayerToward(Minecraft minecraft, Vec3 direction) {
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (minecraft.player == null || flat.lengthSqr() <= 1.0E-6D) {
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
        minecraft.player.setYRot(yaw);
        minecraft.player.setYHeadRot(yaw);
    }

    private ClientEvents() {
    }
}
