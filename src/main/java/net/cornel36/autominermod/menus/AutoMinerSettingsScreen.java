package net.cornel36.autominermod.menus;

import net.cornel36.autominermod.menus.AutoMinerSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AutoMinerSettingsScreen extends Screen {

    private final Screen parent;

    private ButtonWidget modeButton;

    public AutoMinerSettingsScreen(Screen parent) {
        super(Text.literal("AutoMiner Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 2;
        int y = this.height / 4;

        modeButton = ButtonWidget.builder(Text.literal(getModeButtonText()), button -> {
            // change mode cyclically
            AutoMinerSettings.Mode newMode = switch (AutoMinerSettings.getMode()) {
                case AREA -> AutoMinerSettings.Mode.STRAIGHT;
                case STRAIGHT -> AutoMinerSettings.Mode.MOB_GRINDER;
                case MOB_GRINDER -> AutoMinerSettings.Mode.AREA;
            };
            AutoMinerSettings.setMode(newMode);

            // update button text
            modeButton.setMessage(Text.literal(getModeButtonText()));
            // refresh to show options for new mode
            this.refreshCustomizationOptions();
        }).dimensions(x, y, buttonWidth, buttonHeight).build();

        this.addDrawableChild(modeButton);

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            this.client.setScreen(parent);
        }).dimensions(x, this.height - 30, buttonWidth, buttonHeight).build());

        refreshCustomizationOptions();
    }

    private String getModeButtonText() {
        return "Mode: " + switch (AutoMinerSettings.getMode()) {
            case AREA -> "Area Mining";
            case STRAIGHT -> "Straight Mining";
            case MOB_GRINDER -> "Mob Grinder";
        };
    }

    private void refreshCustomizationOptions() {
        this.children().removeIf(child -> child != modeButton && !(child instanceof ButtonWidget && ((ButtonWidget) child).getMessage().getString().equals("Back")));

        int x = (this.width - 200) / 2;
        int y = this.height / 4 + 40;

        switch (AutoMinerSettings.getMode()) {
            case AREA -> {
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Area Mining options here"),
                        b -> {}
                ).dimensions(x, y, 200, 20).build());
            }
            case STRAIGHT -> {
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Straight Mining options here"),
                        b -> {}
                ).dimensions(x, y, 200, 20).build());
            }
            case MOB_GRINDER -> {
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Mob Grinder options here"),
                        b -> {}
                ).dimensions(x, y, 200, 20).build());
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        int x = (this.width - this.textRenderer.getWidth(this.title)) / 2;
        context.drawText(this.textRenderer, this.title, x, 20, 0xFFFFFF, true);
        super.render(context, mouseX, mouseY, tickDelta);
    }
}
