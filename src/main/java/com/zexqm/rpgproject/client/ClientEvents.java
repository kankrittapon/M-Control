package com.zexqm.rpgproject.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.ClientMana;
import com.zexqm.rpgproject.network.RpgNetwork;
import com.zexqm.rpgproject.network.ToggleCombatPacket;
import com.zexqm.rpgproject.network.CancelSkillActionPacket;
import com.zexqm.rpgproject.network.CastSkillRequestPacket;
import com.zexqm.rpgproject.network.BeginSkillTargetingPacket;
import com.zexqm.rpgproject.network.SkillTargetAimPacket;
import com.zexqm.rpgproject.rpg.skill.SkillAimMode;
import com.zexqm.rpgproject.rpg.skill.SkillCancelTrigger;
import com.zexqm.rpgproject.rpg.skill.SkillActionState;
import com.zexqm.rpgproject.rpg.skill.MovementPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final long DOUBLE_CLICK_MS = 350L;
    private static long lastLeftClickTime;
    private static long lastForwardTapTime;
    private static long suppressTeleportJumpUntil;
    private static long targetingRequestExpiresAt;
    private static ResourceLocation targetingRequestSkill;
    private static int lastLeftClickEntityId = -1;
    private static BlockPos lastLeftClickBlockPos;
    private static boolean movementCancelSent;
    private static boolean suppressNextLeftRelease;
    private static final ResourceLocation EARTHS_RESPONSE =
            new ResourceLocation(RpgProject.MOD_ID, "wizard_earth_s_response");
    private static final ResourceLocation WIZARD_STAFF_ATTACK =
            new ResourceLocation(RpgProject.MOD_ID, "wizard_staff_attack");
    private static final ResourceLocation MAGICAL_EVASION =
            new ResourceLocation(RpgProject.MOD_ID, "wizard_magical_evasion");
    private static final ResourceLocation TELEPORT =
            new ResourceLocation(RpgProject.MOD_ID, "wizard_teleport");

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
    public static final KeyMapping TOGGLE_COMBAT = new KeyMapping("key.rpg_project.toggle_combat",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, "key.categories.rpg_project");
    public static final KeyMapping TOGGLE_DEBUG_HUD = new KeyMapping("key.rpg_project.toggle_debug_hud",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.rpg_project");

    @Mod.EventBusSubscriber(modid = RpgProject.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_VIEW_MODE);
            event.register(TOGGLE_MOUSE_MOVEMENT);
            event.register(TOGGLE_COMBAT);
            event.register(TOGGLE_DEBUG_HUD);
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
        while (TOGGLE_DEBUG_HUD.consumeClick()) ClientDebugHud.toggle(minecraft);

        ClientCameraController.tick(minecraft);
        ClientControlState.updateMouseMovementMode(minecraft, TOGGLE_MOUSE_MOVEMENT.isDown());
        if (ClientControlState.isThirdPerson()) synchronizeCombatHitResult(minecraft);
        // Phase 5: Auto-mantle (Set step height to 1.25 blocks so player steps up automatically)
        if (minecraft.player != null) {
            minecraft.player.setMaxUpStep(1.25F);
        }

        ClientControlState.tick(minecraft);
        ClientTargeting.tick(minecraft);
        ClientCombatState.tick();
        ClientDebugHud.tick(minecraft);
        if (ClientSkillState.action() != SkillActionState.CASTING) movementCancelSent = false;
        if (targetingRequestSkill != null && ClientSkillState.action() == SkillActionState.TARGETING) {
            targetingRequestSkill = null;
            targetingRequestExpiresAt = 0;
        } else if (targetingRequestSkill != null
                && System.currentTimeMillis() >= targetingRequestExpiresAt) {
            targetingRequestSkill = null;
            targetingRequestExpiresAt = 0;
        }

        if (combatInputActive()) {
            // Shift/Ctrl are RPG combo and cursor modifiers while the weapon is drawn.
            // Keep auto-move sprint state intact; only suppress vanilla key actions.
            minecraft.options.keyShift.setDown(false);
            minecraft.options.keySprint.setDown(false);
            if (!ClientControlState.isThirdPerson()) synchronizeCombatHitResult(minecraft);
        }

        if (ClientCombatState.actionLocked()) {
            ClientControlState.cancelAutoMove();
            minecraft.options.keyAttack.setDown(false);
            minecraft.options.keyUse.setDown(false);
            minecraft.options.keyJump.setDown(false);
            return;
        }

        while (TOGGLE_COMBAT.consumeClick()) RpgNetwork.CHANNEL.sendToServer(new ToggleCombatPacket());

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
        if (minecraft.screen != null) {
            suppressNextLeftRelease = false;
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getAction() == GLFW.GLFW_PRESS
                && (ClientSkillState.action() == SkillActionState.TARGETING
                || targetingRequestSkill != null)) {
            suppressNextLeftRelease = true;
            ResourceLocation targetSkill = ClientSkillState.action() == SkillActionState.TARGETING
                    ? ClientSkillState.activeSkill() : targetingRequestSkill;
            if (ClientControlState.isMouseMovementMode() && targetSkill != null) {
                ClientControlState.cancelAutoMove();
                sendTargetAim(minecraft, targetSkill, true);
                targetingRequestSkill = null;
                targetingRequestExpiresAt = 0;
            } else if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("Hold Ctrl + LMB to confirm target"), true);
            }
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getAction() == GLFW.GLFW_PRESS
                && ClientSkillState.action() == SkillActionState.CASTING
                && ClientSkillState.aimMode() == SkillAimMode.CHANNEL_TARGETING
                && ClientControlState.isMouseMovementMode()
                && ClientSkillState.activeSkill() != null) {
            suppressNextLeftRelease = true;
            sendTargetAim(minecraft, ClientSkillState.activeSkill(), false);
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && event.getAction() == GLFW.GLFW_PRESS
                && (ClientSkillState.action() == SkillActionState.TARGETING
                || targetingRequestSkill != null)) {
            targetingRequestSkill = null;
            targetingRequestExpiresAt = 0;
            RpgNetwork.CHANNEL.sendToServer(new CancelSkillActionPacket(SkillCancelTrigger.MOVEMENT));
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getAction() == GLFW.GLFW_PRESS
                && tryDirectionalCombatSkill(minecraft)) {
            suppressNextLeftRelease = true;
            minecraft.options.keyAttack.setDown(false);
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getAction() == GLFW.GLFW_PRESS
                && !ClientControlState.isMouseMovementMode()
                && tryBasicCombatSkill(minecraft)) {
            suppressNextLeftRelease = true;
            minecraft.options.keyAttack.setDown(false);
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getAction() == GLFW.GLFW_RELEASE && suppressNextLeftRelease) {
            suppressNextLeftRelease = false;
            event.setCanceled(true);
            return;
        }

        if (!ClientControlState.isMouseMovementMode()) return;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_RELEASE) {
            handleLeftClick(minecraft);
            event.setCanceled(true);
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (!requestMovementCancelIfNeeded()) {
                event.setCanceled(true);
                return;
            }
            handleRightClick(minecraft);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void combatMovementSkill(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null
                || ClientSkillState.action() != SkillActionState.READY
                || ClientCombatState.actionLocked()) return;

        if (event.getKey() == GLFW.GLFW_KEY_SPACE && isShiftDown(minecraft)) {
            suppressTeleportJumpUntil = System.currentTimeMillis() + 250L;
            minecraft.options.keyJump.setDown(false);
            int axis = minecraft.options.keyUp.isDown() ? 1
                    : minecraft.options.keyDown.isDown() ? -1 : 0;
            if (axis == 0) {
                RpgNetwork.CHANNEL.sendToServer(new BeginSkillTargetingPacket(TELEPORT));
                targetingRequestSkill = TELEPORT;
                targetingRequestExpiresAt = System.currentTimeMillis() + 1000L;
                minecraft.player.displayClientMessage(Component.literal("Teleport: select destination"), true);
            } else {
                ClientControlState.cancelAutoMove();
                sendAimSkill(minecraft, TELEPORT, 0, axis);
            }
            return;
        }

        int side = event.getKey() == GLFW.GLFW_KEY_A ? -1 : event.getKey() == GLFW.GLFW_KEY_D ? 1 : 0;
        int forward = event.getKey() == GLFW.GLFW_KEY_S ? -1 : 0;
        boolean cast = (side != 0 || forward != 0) && isShiftDown(minecraft);
        if (event.getKey() == GLFW.GLFW_KEY_W) {
            long now = System.currentTimeMillis();
            cast = now - lastForwardTapTime <= DOUBLE_CLICK_MS;
            lastForwardTapTime = now;
            if (cast) forward = 1;
        }
        if (!cast) return;

        ClientControlState.cancelAutoMove();
        sendAimSkill(minecraft, MAGICAL_EVASION, side, forward);
        // Forge 1.20.1 InputEvent.Key is observational and cannot be canceled.
        // The server-authoritative dodge replaces the movement produced by this key press.
        if (event.isCancelable()) event.setCanceled(true);
    }

    private static boolean tryDirectionalCombatSkill(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null
                || ClientSkillState.action() != SkillActionState.READY
                || ClientCombatState.actionLocked()) return false;
        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();
        if (left == right) return false;

        int side = left ? -1 : 1;
        ClientControlState.cancelAutoMove();
        sendAimSkill(minecraft, EARTHS_RESPONSE, side);
        return true;
    }

    private static boolean tryBasicCombatSkill(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null
                || ClientSkillState.action() != SkillActionState.READY
                || ClientCombatState.actionLocked()) return false;
        ClientControlState.cancelAutoMove();
        sendAimSkill(minecraft, WIZARD_STAFF_ATTACK, 0);
        return true;
    }

    private static void sendAimSkill(Minecraft minecraft, ResourceLocation skillId, int lateralSide) {
        sendAimSkill(minecraft, skillId, lateralSide,
                minecraft.options.keyUp.isDown() ? 1 : minecraft.options.keyDown.isDown() ? -1 : 0);
    }

    private static void sendAimSkill(Minecraft minecraft, ResourceLocation skillId,
                                     int lateralSide, int forwardAxis) {
        ClientAim.AimResult aim = ClientAim.current(minecraft);
        Integer targetId = aim.entityHit() == null ? null : aim.entityHit().getEntity().getId();
        Vec3 ground = aim.blockHit() != null && aim.blockHit().getType() != HitResult.Type.MISS
                ? aim.blockHit().getLocation() : aim.targetPoint();
        RpgNetwork.CHANNEL.sendToServer(new CastSkillRequestPacket(
                skillId, aim.direction(), targetId, ground, lateralSide, forwardAxis));
    }

    private static void sendTargetAim(Minecraft minecraft, ResourceLocation skillId, boolean confirm) {
        ClientAim.AimResult aim = ClientAim.current(minecraft);
        Integer targetId = aim.entityHit() == null ? null : aim.entityHit().getEntity().getId();
        Vec3 ground = aim.blockHit() != null && aim.blockHit().getType() != HitResult.Type.MISS
                ? aim.blockHit().getLocation() : aim.targetPoint();
        RpgNetwork.CHANNEL.sendToServer(new SkillTargetAimPacket(
                skillId, confirm, aim.direction(), targetId, ground));
        spawnGroundClickMarker(minecraft, ground);
    }

    @SubscribeEvent
    public static void blockAttack(PlayerInteractEvent.LeftClickBlock event) {
        if (isLocalThirdPersonInteraction(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void blockUse(PlayerInteractEvent.RightClickBlock event) {
        if (isLocalThirdPersonInteraction(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
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
        if (combatInputActive()) event.getInput().shiftKeyDown = false;
        if (System.currentTimeMillis() < suppressTeleportJumpUntil) event.getInput().jumping = false;
        MovementPolicy skillMovement = ClientSkillState.movement();
        boolean movementRequested = event.getInput().forwardImpulse != 0.0F
                || event.getInput().leftImpulse != 0.0F || event.getInput().jumping
                || ClientControlState.isAutoMoving();
        boolean cancelingCast = movementRequested && ClientSkillState.movementCancelAllowed();
        if (cancelingCast) requestMovementCancelIfNeeded();
        if (ClientCombatState.actionLocked()
                || (skillMovement == MovementPolicy.LOCKED && !cancelingCast)
                || (skillMovement == MovementPolicy.ROTATE_ONLY && !cancelingCast)) {
            ClientControlState.cancelAutoMove();
            event.getInput().up = false;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
            event.getInput().jumping = false;
            event.getInput().forwardImpulse = 0.0F;
            event.getInput().leftImpulse = 0.0F;
            return;
        }
        if (skillMovement == MovementPolicy.WALK) {
            ClientControlState.cancelAutoMove();
            event.getInput().forwardImpulse *= 0.5F;
            event.getInput().leftImpulse *= 0.5F;
        }
        if (ClientControlState.isAutoMoving()) {
            float throttle = ClientControlState.autoMoveThrottle();
            event.getInput().up = throttle > 0.0F;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
            event.getInput().forwardImpulse = throttle;
            event.getInput().leftImpulse = 0.0F;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !ClientControlState.isThirdPerson()
                || player.isInWaterOrBubble() || player.isInLava()) return;

        float strafe = event.getInput().leftImpulse;
        float forward = event.getInput().forwardImpulse;
        if (strafe == 0.0F && forward == 0.0F) return;

        // Convert camera-relative input into player-local vanilla impulses. This
        // preserves diagonal movement without rotating the body or the camera.
        float delta = (ClientCameraController.getRawYaw() - player.getYRot()) * ((float) Math.PI / 180.0F);
        float cos = (float) Math.cos(delta);
        float sin = (float) Math.sin(delta);
        event.getInput().leftImpulse = strafe * cos - forward * sin;
        event.getInput().forwardImpulse = forward * cos + strafe * sin;
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
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        ClientDebugHud.render(minecraft, graphics, width);

        if (ClientSkillState.action() == com.zexqm.rpgproject.rpg.skill.SkillActionState.CASTING) {
            var icon = ClientSkillIcons.icon(ClientSkillState.activeSkill());
            if (icon != null) {
                int size = 32;
                int x = (width - size) / 2;
                int y = height - 98;
                graphics.fill(x - 2, y - 2, x + size + 2, y + size + 5, 0xCC0D1116);
                graphics.blit(icon, x, y, 0, 0, size, size, size, size);
                int total = Math.max(1, ClientSkillState.castTicks());
                int elapsed = Math.max(0, total - ClientSkillState.actionTicks());
                int progress = Math.min(size, Math.round(size * elapsed / (float) total));
                graphics.fill(x, y + size + 1, x + progress, y + size + 3, 0xFFFFA632);
            }
        }

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
                } else {
                    // Normal crosshair
                    graphics.fill(width / 2 - 1, height / 2 - 1, width / 2 + 1, height / 2 + 1, 0xAAFFFFFF);
                }
            } else {
                // Mouse Mode: HUD indicator
                graphics.drawCenteredString(minecraft.font, Component.translatable("message.rpg_project.mouse_mode.hud"), width / 2, height / 2 + 40, 0xFFE6F7FF);
            }
        }

        double staminaMax = ClientRpgData.maxStamina();
        if (ClientControlState.isThirdPerson() && staminaMax > 0 && ClientRpgData.stamina() < staminaMax - 0.5) {
            int barWidth = 120;
            int x = (width - barWidth) / 2;
            int y = height - 58;
            int filled = (int) Math.round(barWidth * Math.max(0.0, ClientRpgData.stamina()) / staminaMax);
            graphics.fill(x - 1, y - 1, x + barWidth + 1, y + 6, 0xAA101418);
            graphics.fill(x, y, x + filled, y + 5, 0xFFE2C24F);
        }
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
            rememberBlockClick(pos, now);
        } else {
            resetDoubleClick();
        }

        ClientTargeting.clear();
        minecraft.player.displayClientMessage(Component.translatable("message.rpg_project.target.cleared"), true);
    }

    private static boolean isLocalThirdPersonInteraction(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return player.level().isClientSide
                && minecraft.player == player
                && ClientControlState.isThirdPerson();
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

    private static boolean requestMovementCancelIfNeeded() {
        if (ClientSkillState.action() != SkillActionState.CASTING) return true;
        if (!ClientSkillState.movementCancelAllowed()) return false;
        if (!movementCancelSent) {
            movementCancelSent = true;
            RpgNetwork.CHANNEL.sendToServer(new CancelSkillActionPacket(SkillCancelTrigger.MOVEMENT));
        }
        return true;
    }

    private static boolean shouldZoomCamera(Minecraft minecraft) {
        return ClientControlState.isMouseMovementMode() || isShiftDown(minecraft);
    }

    private static boolean isShiftDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean combatInputActive() {
        return ClientSkillState.action() != SkillActionState.SHEATHED
                && ClientSkillState.action() != SkillActionState.SHEATHING;
    }

    private static void synchronizeCombatHitResult(Minecraft minecraft) {
        ClientAim.AimResult aim = ClientAim.current(minecraft);
        double entityDistance = aim.entityHit() == null ? Double.MAX_VALUE
                : aim.origin().distanceToSqr(aim.entityHit().getLocation());
        double blockDistance = aim.blockHit() == null || aim.blockHit().getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE : aim.origin().distanceToSqr(aim.blockHit().getLocation());
        if (entityDistance < blockDistance) {
            minecraft.crosshairPickEntity = aim.entityHit().getEntity();
            minecraft.hitResult = aim.entityHit();
        } else {
            minecraft.crosshairPickEntity = null;
            minecraft.hitResult = aim.blockHit();
        }
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
