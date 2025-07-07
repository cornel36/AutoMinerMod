package net.cornel36.autominermod;

import net.cornel36.autominermod.visual.AutoMinerHUDOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import net.cornel36.autominermod.selection.AreaSelector;

/**
 * Main mod class for AutoMinerMod.
 * Initializes key bindings, event listeners, and handles
 * toggling of the automatic mining task within a selected area.
 */
public class AutoMinerMod implements ClientModInitializer {

	private static KeyBinding toggleKey;
	public static AutoMinerTask autoMinerTask;
	private static boolean wasPressed = false;

	/**
	 * Initializes the client mod. This method is called automatically by Fabric
	 * when the client starts. Sets up the key binding, area selection, event listeners,
	 * and initializes the mining task.
	 */
	@Override
	public void onInitializeClient() {
		// Register toggle keybinding (default key: K)
		toggleKey = new KeyBinding("key.autominermod.toggle", // Localization key
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				"key.categories.misc");
		KeyBindingHelper.registerKeyBinding(toggleKey);

		// Register custom area selector and key handler
		AreaSelector.register();
		KeyHandler.register();

		// Initialize the AutoMiner task
		autoMinerTask = new AutoMinerTask(MinecraftClient.getInstance());

		// Register a tick event that checks key state and controls mining task
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Detect fresh key press (edge detection)
			if (toggleKey.isPressed()) {
				if (!wasPressed) {
					wasPressed = true;

					if (!autoMinerTask.isRunning()) {
						// Attempt to start AutoMiner if both positions are selected
						BlockPos pos1 = AreaSelector.getPos1();
						BlockPos pos2 = AreaSelector.getPos2();

						if (pos1 != null && pos2 != null) {
							autoMinerTask.start(pos1, pos2);
							client.player.sendMessage(Text.literal("AutoMiner started."), false);
						} else {
							client.player.sendMessage(Text.literal("Set both pos1 and pos2 with wooden sword."), false);
						}
					} else {
						// Stop AutoMiner if it’s already running
						autoMinerTask.stop();
						client.player.sendMessage(Text.literal("AutoMiner stopped."), false);
					}
				}
			} else {
				wasPressed = false; // Reset press detection
			}
			// Run AutoMiner logic every tick (even when idle)
			autoMinerTask.tick();
		});

		// Register HUD overlay renderer for displaying status info
		HudRenderCallback.EVENT.register((drawContext,
										  tickDelta) -> {
			AutoMinerHUDOverlay.render(drawContext, autoMinerTask);
		});
	}
	/**
	 * Returns whether the AutoMiner task is currently active.
	 * @return true if the AutoMiner is running, false otherwise
	 */
	public static boolean isActive() {
		return autoMinerTask != null && autoMinerTask.isRunning();
	}
}
