package net.cornel36.autominermod;

import net.cornel36.autominermod.menus.AutoMinerSettings;
import net.cornel36.autominermod.menus.AutoMinerSettingsScreen;
import net.cornel36.autominermod.mixin.MixinScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
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
		autoMinerTask = new AutoMinerTask(MinecraftClient.getInstance());

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.autominermod.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				"category.autominermod.general"
		));
		AreaSelector.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			if (toggleKey.isPressed()) {
				if (!wasPressed) {
					wasPressed = true;
					if (!autoMinerTask.isRunning()) {
						AutoMinerSettings.Mode currentMode = AutoMinerSettings.getMode();

						if (currentMode == AutoMinerSettings.Mode.AREA) {
							BlockPos pos1 = AreaSelector.getPos1();
							BlockPos pos2 = AreaSelector.getPos2();
							if (pos1 != null && pos2 != null) {
								autoMinerTask.start(currentMode, pos1, pos2);
							} else {
								client.player.sendMessage(Text.literal("Set both pos1 and pos2 with wooden sword."), false);
							}
						} else if (currentMode == AutoMinerSettings.Mode.STRAIGHT) {
							autoMinerTask.start(currentMode, null, null);
						} else if (currentMode == AutoMinerSettings.Mode.MOB_GRINDER) {
							autoMinerTask.start(currentMode, null, null);
						}

						if (autoMinerTask.isRunning()) {
							client.player.sendMessage(Text.literal("AutoMiner started in " + currentMode.name() + " mode."), false);
						}
					} else {
						autoMinerTask.stop();
						client.player.sendMessage(Text.literal("AutoMiner stopped."), false);
					}
				}
			} else {
				wasPressed = false;
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

		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			if (autoMinerTask != null && autoMinerTask.isRunning() && autoMinerTask.getMode() != null) {
				System.out.println("Rendering HUD overlay");
				var debugLines = autoMinerTask.getMode().getDebugText();
				var textRenderer = MinecraftClient.getInstance().textRenderer;

				int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
				int marginRight = 5;
				int marginTop = 5;


				String activeText = "AutoMiner Active";
				int activeTextWidth = textRenderer.getWidth(activeText);
				int xActive = width - marginRight - activeTextWidth;
				int yActive = marginTop;
				int greenColor = 0xFF00FF00;

				drawContext.drawText(textRenderer, activeText, xActive, yActive, greenColor, true);

				for (int i = 0; i < debugLines.size(); i++) {
					String line = debugLines.get(i);
					int lineWidth = textRenderer.getWidth(line);
					int x = width - marginRight - lineWidth;
					int y = yActive + 12 + i * 10;
					drawContext.drawText(textRenderer, line, x, y, 0xFFFFFFFF, true);
				}
			}
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
