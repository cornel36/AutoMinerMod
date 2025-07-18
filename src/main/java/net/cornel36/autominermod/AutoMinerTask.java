package net.cornel36.autominermod;

import net.cornel36.autominermod.menus.AutoMinerSettings;
import net.cornel36.autominermod.modes.MiningMode;
import net.cornel36.autominermod.modes.MobGrinderMode;
import net.cornel36.autominermod.modes.StraightMiningMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.cornel36.autominermod.modes.AreaMiningMode;

/**
 * Represents a task that automatically mines a selected 3D area in layers from top to bottom.
 * Handles block targeting, mining, movement logic, tool selection, and HUD debugging information.
 */
public class AutoMinerTask {
    public enum State {
        IDLE,
        MINING,
        DONE,
        GRINDING
    }

    // Constants
    public static final double REACH_DISTANCE_SQ = 25.0;

    private final MinecraftClient client;
    private State state = State.IDLE;

    // Area selection
    private BlockPos areaPos1;
    private BlockPos areaPos2;
    private int minY;
    private int startY;

    // Active mining mode
    private MiningMode mode;

    public AutoMinerTask(MinecraftClient client) {
        this.client = client;
    }

    public void start(AutoMinerSettings.Mode currentMode, BlockPos pos1, BlockPos pos2) {
        switch (currentMode) {
            case AREA:
                if (pos1 == null || pos2 == null) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("Error: Area mining requires both positions to be set first."), false);
                    }
                    this.state = State.IDLE;
                    return;
                }
                this.minY = Math.min(pos1.getY(), pos2.getY());
                this.startY = Math.max(pos1.getY(), pos2.getY());
                mode = new AreaMiningMode(client, pos1, pos2, this.minY, this.startY);
                mode.start(pos1, pos2);
                break;
            case STRAIGHT:
                mode = new StraightMiningMode(client);
                mode.start(null, null);
                break;
            case MOB_GRINDER:
                mode = new MobGrinderMode(client);
                mode.start(null, null);
                break;
        }
        if (mode != null) {
            state = State.MINING;
        }
    }

        public BlockPos getCurrentTarget () {
            return mode != null ? mode.getCurrentTarget() : null;
        }

        public void tick () {
            if (client.player == null || client.world == null) return;
            if (state != State.MINING) return;

            if (mode != null) {
                mode.tick();
            }
        }

        public void stop () {
            this.state = State.IDLE;
            this.mode = null;
        }

        public boolean isRunning () {
            return state == State.MINING;
        }

        public int getCurrentY () {
            return mode != null ? mode.getCurrentY() : 0;
        }

        public int getMinY () {
            return minY;
        }

        public int getTotalBlockCount () {
            return mode != null ? mode.getTotalBlockCount() : 0;
        }

        public int getRemainingBlocks () {
            return mode != null ? mode.getRemainingBlocks() : 0;
        }

        public MiningMode getMode () {
            return mode;
        }

        public State getState () {
            return state;
        }

    }
