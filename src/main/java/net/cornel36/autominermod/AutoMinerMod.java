package net.cornel36.autominermod;

import net.cornel36.autominermod.menus.AutoMinerSettings;
import net.cornel36.autominermod.menus.AutoMinerSettingsScreen;
import net.cornel36.autominermod.mixin.MixinScreenAccessor;
import net.cornel36.autominermod.visual.AutoMinerHUDOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import net.cornel36.autominermod.selection.AreaSelector;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * Main mod class for AutoMinerMod.
 * Initializes key bindings, event listeners, and handles
 * toggling of the automatic mining task within a selected area.
 */
public class AutoMinerMod implements ClientModInitializer {

	private static final Identifier HUD_LAYER = Identifier.of("autominermod", "auto_miner_hud");
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
		toggleKey = new KeyBinding("key.autominermod.toggle",
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
							autoMinerTask.setMode(AutoMinerSettings.getMode());

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
			autoMinerTask.tick();
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof OptionsScreen) {
				ButtonWidget button = ButtonWidget.builder(
						Text.literal("AutoMinerMod Settings"),
						btn -> client.setScreen(new AutoMinerSettingsScreen(screen))
				).dimensions(screen.width / 2 - 100, screen.height / 6 + 144, 200, 20).build();

				MixinScreenAccessor accessor = (MixinScreenAccessor) screen;
				accessor.getDrawables().add(button);
				accessor.getChildren().add(button);
			}
		});
		// Register HUD overlay renderer for displaying status info
		HudRenderCallback.EVENT.register(AutoMinerHUDOverlay::render);
	}

	/**
	 * Returns whether the AutoMiner task is currently active.
	 * @return true if the AutoMiner is running, false otherwise
	 */
	public static boolean isActive() {
		return autoMinerTask != null && autoMinerTask.isRunning();
	}
}
