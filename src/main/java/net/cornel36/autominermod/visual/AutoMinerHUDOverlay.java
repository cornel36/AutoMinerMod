package net.cornel36.autominermod.visual;

import net.cornel36.autominermod.AutoMinerMod;
import net.cornel36.autominermod.AutoMinerTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * HUD overlay renderer for the AutoMiner mod.
 * Displays the status of the miner and debug information during operation.
 */
public class AutoMinerHUDOverlay {

    /**
     * Renders HUD elements for the AutoMiner task.
     * Shows active status in the top-left corner and debug info below it.
     *
     * @param context The current draw context for rendering text
     */
    public static void render(DrawContext context, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        AutoMinerTask task = AutoMinerMod.autoMinerTask;
        if (task == null || !task.isRunning()) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int x;
        int y = 10;
        int lineHeight = 12;

        // Show status text
        String statusText = "AutoMiner Active";
        x = screenWidth - 10 - client.textRenderer.getWidth(statusText);
        context.drawText(client.textRenderer, statusText, x, y, 0xFF00FF00, true);
        y += lineHeight;

        // Show layers left to dig
        int layersLeft = task.getCurrentY() - task.getMinY() + 1;
        String layersText = "Layers left: " + layersLeft;
        x = screenWidth - 10 - client.textRenderer.getWidth(layersText);
        context.drawText(client.textRenderer, layersText, x, y, 0xFFFFFFFF, true);
        y += lineHeight;

        // show how many blocks are left to mine
        int remainingBlocks = task.getRemainingBlocks();
        int totalBlocks = task.getTotalBlockCount();
        String blocksText = "Blocks left: " + remainingBlocks + " / " + totalBlocks;
        x = screenWidth - 10 - client.textRenderer.getWidth(blocksText);
        context.drawText(client.textRenderer, blocksText, x, y, 0xFFFFFFFF, true);
        y += lineHeight;

        // current target
        BlockPos target = task.getCurrentTarget();
        String targetText = "Target: " + (target != null ? target.toShortString() : "None");
        x = screenWidth - 10 - client.textRenderer.getWidth(targetText);
        context.drawText(client.textRenderer, targetText, x, y, 0xFFFFFFFF, true);
    }

}
