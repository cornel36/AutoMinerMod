package net.cornel36.autominermod.visual;

import net.cornel36.autominermod.AutoMinerTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * HUD overlay renderer for the AutoMiner mod.
 * Displays the status of the miner and debug information during operation.
 */
public class AutoMinerHUDOverlay {

    /**
     * Renders HUD elements for the AutoMiner task.
     * Shows active status in the top-right corner and debug info in the bottom-left.
     *
     * @param drawContext The current draw context for rendering text
     * @param task        The active AutoMiner task instance
     */
    public static void render(DrawContext drawContext, AutoMinerTask task) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // ── Top-right corner: "AutoMiner Active" status ──
        if (task != null && task.isRunning()) {
            String activeText = "AutoMiner Active";
            int textWidth = client.textRenderer.getWidth(activeText);
            int padding = 10;

            drawContext.drawText(
                client.textRenderer,
                activeText,
                screenWidth - textWidth - padding,
                padding,
                0x00FF00, // Green text color
                true
            );
        }

        // ── Bottom-left corner: debug lines ──
        if (task != null && task.isRunning()) {
            List<String> lines = task.getDebugText();

            int x = 10;
            int yOffsetFromBottom = 30;  // Space from bottom of screen
            int lineHeight = 10;

            int totalHeight = lines.size() * lineHeight;
            int y = screenHeight - totalHeight - yOffsetFromBottom;

            for (String line : lines) {
                drawContext.drawText(
                    client.textRenderer,
                    line,
                    x,
                    y,
                    0xFFFFFF, // White text color
                    true
                );
                y += lineHeight;
            }
        }
    }
}
