package com.zexqm.rpgproject.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CameraSettingsScreen extends Screen {
    private final Screen parent;

    public CameraSettingsScreen(Screen parent) {
        super(Component.translatable("screen.rpg_project.camera.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int top = this.height / 2 - 45;
        addRenderableWidget(new CameraDistanceSlider(left, top, 200, 20));
        addRenderableWidget(new DragSensitivitySlider(left, top + 28, 200, 20));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left, top + 68, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 75, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private static final class CameraDistanceSlider extends AbstractSliderButton {
        private CameraDistanceSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), normalizedDistance());
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.translatable("screen.rpg_project.camera.distance",
                    String.format("%.1f", ClientCameraController.getCameraDistance())));
        }

        @Override protected void applyValue() {
            double min = ClientCameraController.getMinDistance();
            double max = ClientCameraController.getMaxDistance();
            ClientCameraController.setCameraDistance(min + value * (max - min));
        }

        private static double normalizedDistance() {
            double min = ClientCameraController.getMinDistance();
            double max = ClientCameraController.getMaxDistance();
            return (ClientCameraController.getCameraDistance() - min) / (max - min);
        }
    }

    private static final class DragSensitivitySlider extends AbstractSliderButton {
        private static final double MIN = 0.03D;
        private static final double MAX = 0.5D;

        private DragSensitivitySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    (ClientCameraController.getDragSensitivity() - MIN) / (MAX - MIN));
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.translatable("screen.rpg_project.camera.drag_sensitivity",
                    Math.round(ClientCameraController.getDragSensitivity() * 100.0D)));
        }

        @Override protected void applyValue() {
            ClientCameraController.setDragSensitivity(MIN + value * (MAX - MIN));
        }
    }
}
