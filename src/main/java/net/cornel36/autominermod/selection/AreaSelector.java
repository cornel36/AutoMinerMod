package net.cornel36.autominermod.selection;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;

/**
 * Handles block-based area selection using a wooden sword.
 * Left-click sets position 1 (pos1), right-click sets position 2 (pos2).
 * Selection is active only when the player is holding a wooden sword.
 */
public class AreaSelector {

    private static BlockPos pos1 = null;
    private static BlockPos pos2 = null;
    private static boolean leftClickHeld = false;
    private static boolean rightClickHeld = false;

    /**
     * Registers the area selector logic to the client tick event.
     * When the player holds a wooden sword, left- and right-clicks will
     * select two corner positions of a mining area.
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Abort if not in-game or world not loaded
            if (client == null || client.player == null || client.world == null) return;

            // Only activate if player is holding a wooden sword
            if (client.player.getMainHandStack().getItem() != Items.WOODEN_SWORD) return;

            // Handle left click (attack) — sets pos1
            if (client.options.attackKey.isPressed()) {
                if (!leftClickHeld) {
                    onLeftClick(client);
                    leftClickHeld = true;
                }
            } else {
                leftClickHeld = false;
            }

            // Handle right click (use) — sets pos2
            if (client.options.useKey.isPressed()) {
                if (!rightClickHeld) {
                    onRightClick(client);
                    rightClickHeld = true;
                }
            } else {
                rightClickHeld = false;
            }
        });
    }

    /**
     * Handles left-click selection for pos1.
     * @param client The active Minecraft client instance
     */
    private static void onLeftClick(MinecraftClient client) {
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            pos1 = hit.getBlockPos();
            client.player.sendMessage(Text.literal("Set pos1 to " + pos1), false);
        }
    }

    /**
     * Handles right-click selection for pos2.
     * @param client The active Minecraft client instance
     */
    private static void onRightClick(MinecraftClient client) {
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            pos2 = hit.getBlockPos();
            client.player.sendMessage(Text.literal("Set pos2 to " + pos2), false);
        }
    }

    /**
     * @return The first selected block position (pos1), or null if not set.
     */
    public static BlockPos getPos1() {
        return pos1;
    }

    /**
     * @return The second selected block position (pos2), or null if not set.
     */
    public static BlockPos getPos2() {
        return pos2;
    }

    /**
     * Sets the first position manually (optional usage).
     * @param pos The new pos1 value
     */
    public static void setPos1(BlockPos pos) {
        pos1 = pos;
    }

    /**
     * Sets the second position manually (optional usage).
     * @param pos The new pos2 value
     */
    public static void setPos2(BlockPos pos) {
        pos2 = pos;
    }
}
