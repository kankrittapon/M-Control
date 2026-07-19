package com.zexqm.rpgproject.client;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.ClientMana;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Development-only player state HUD with rate-limited structured logging. */
public final class ClientDebugHud {
    private static boolean enabled;
    private static String lastSnapshot = "";
    private static long lastLogTick = Long.MIN_VALUE;

    public static void toggle(Minecraft minecraft) {
        enabled = !enabled;
        lastSnapshot = "";
        lastLogTick = Long.MIN_VALUE;
        if (minecraft.player != null) minecraft.player.displayClientMessage(Component.literal(
                "RPG Debug HUD: " + (enabled ? "ON" : "OFF")), true);
        RpgProject.LOGGER.info("[RPG Debug HUD] enabled={}", enabled);
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void tick(Minecraft minecraft) {
        if (!enabled || minecraft.player == null || minecraft.level == null) return;
        String snapshot = snapshot(minecraft);
        long tick = minecraft.level.getGameTime();
        if (!snapshot.equals(lastSnapshot) && (lastLogTick == Long.MIN_VALUE || tick - lastLogTick >= 20)) {
            RpgProject.LOGGER.info("[RPG Debug HUD] {}", snapshot);
            lastSnapshot = snapshot;
            lastLogTick = tick;
        }
    }

    public static void render(Minecraft minecraft, GuiGraphics graphics, int screenWidth) {
        if (!enabled || minecraft.player == null) return;
        List<String> lines = lines(minecraft);
        final float scale = 0.75F;
        int lineHeight = minecraft.font.lineHeight + 2;
        int scaledScreenWidth = (int) (screenWidth / scale);
        int panelWidth = 240;
        int x = scaledScreenWidth - panelWidth - 10;
        // Reserve the upper-right region for vanilla/Forge boss bars.
        int y = (int) (68 / scale);
        int height = 10 + lines.size() * lineHeight;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        try {
            graphics.fill(x - 5, y - 5, scaledScreenWidth - 5, y + height, 0xB8101419);
            graphics.fill(x - 5, y - 5, x - 3, y + height, 0xFFE0A33A);
            graphics.drawString(minecraft.font, "RPG DEBUG", x, y, 0xFFFFC45C, false);
            y += lineHeight + 2;
            for (String line : lines) {
                graphics.drawString(minecraft.font, line, x, y, 0xFFE8EDF2, false);
                y += lineHeight;
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static List<String> lines(Minecraft minecraft) {
        var player = minecraft.player;
        List<String> lines = new ArrayList<>();
        String skill = ClientSkillState.activeSkill() == null
                ? "-" : ClientSkillState.activeSkill().getPath();
        lines.add("Combat: " + ClientRpgData.inCombat() + " / " + ClientSkillState.action());
        lines.add("Skill: " + skill + "  ticks=" + ClientSkillState.actionTicks());
        lines.add("Move: " + ClientSkillState.movement() + " / " + ClientControlState.getMovementState());
        lines.add(String.format(Locale.ROOT, "HP: %.1f/%.1f", player.getHealth(), player.getMaxHealth()));
        lines.add("MP: " + ClientMana.mana() + "/" + ClientMana.maxMana());
        lines.add(String.format(Locale.ROOT, "Stamina: %.1f/%.1f",
                ClientRpgData.stamina(), ClientRpgData.maxStamina()));
        lines.add("Level: " + ClientRpgData.level() + "  SP=" + ClientRpgData.skillPoints());
        lines.add(String.format(Locale.ROOT, "Guard: %.1f/%.1f",
                ClientCombatState.guard(), ClientCombatState.maximumGuard()));
        lines.add("Protection: FG=" + ClientCombatState.frontGuardTicks()
                + " SA=" + ClientCombatState.superArmorTicks()
                + " IF=" + ClientCombatState.iframeTicks()
                + " GI=" + ClientCombatState.grabImmuneTicks());
        lines.add(String.format(Locale.ROOT, "CC: %s t=%d points=%.1f immune=%d",
                ClientCombatState.activeCc() == null ? "-" : ClientCombatState.activeCc(),
                ClientCombatState.activeTicks(), ClientCombatState.ccPoints(),
                ClientCombatState.immunityTicks()));
        lines.add("Target: " + (ClientTargeting.targetId() < 0 ? "-"
                : ClientTargeting.targetName() + " #" + ClientTargeting.targetId()));
        var aim = ClientAim.current(minecraft);
        if (aim.entityHit() != null && aim.entityHit().getEntity() instanceof LivingEntity living) {
            lines.add(String.format(Locale.ROOT, "Aim: %s #%d d=%.1f", living.getName().getString(),
                    living.getId(), player.distanceTo(living)));
        } else {
            lines.add("Aim: ground/empty");
        }
        lines.add("View: " + ClientControlState.viewMode()
                + (ClientControlState.isMouseMovementMode() ? " CURSOR" : " ACTION"));
        return lines;
    }

    private static String snapshot(Minecraft minecraft) {
        return String.join(" | ", lines(minecraft));
    }

    private ClientDebugHud() {}
}
