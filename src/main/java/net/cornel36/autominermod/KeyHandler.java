package net.cornel36.autominermod;

import net.cornel36.autominermod.selection.AreaSelector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;

/**
 * Handles custom key input logic for block selection using the wooden sword.
 * This acts as an alternative to AreaSelector's internal click tracking,
 * providing more direct assignment of pos1 and pos2 on attack/use input.
 */
public class KeyHandler {

    // Cached reference to the Minecraft client instance
    private static final MinecraftClient client = MinecraftClient.getInstance();

    /**
     * Registers the tick event that listens for attack/use inputs while
     * the player is holding a wooden sword. Updates AreaSelector pos1/pos2
     * based on crosshair target block positions.
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // Ensure game is in a valid state
            if (client.player == null || client.world == null) return;

            // Only allow block selection when holding a wooden sword
            if (client.player.getMainHandStack().getItem() != Items.WOODEN_SWORD) return;

            // Handle attack key (left click) — set pos1 if changed
            if (client.crosshairTarget instanceof BlockHitResult hit &&
                    client.options.attackKey.isPressed()) {

                BlockPos pos = hit.getBlockPos();
                if (AreaSelector.getPos1() == null || !AreaSelector.getPos1().equals(pos)) {
                    AreaSelector.setPos1(pos);
                    client.player.sendMessage(Text.literal("Set pos1: " + pos.toShortString()), false);
                }
            }

            // Handle use key (right click) — set pos2 if changed
            if (client.crosshairTarget instanceof BlockHitResult hit &&
                    client.options.useKey.isPressed()) {

                BlockPos pos = hit.getBlockPos();
                if (AreaSelector.getPos2() == null || !AreaSelector.getPos2().equals(pos)) {
                    AreaSelector.setPos2(pos);
                    client.player.sendMessage(Text.literal("Set pos2: " + pos.toShortString()), false);
                }
            }
        });
    }
}
