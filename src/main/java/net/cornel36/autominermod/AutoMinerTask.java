package net.cornel36.autominermod;

import net.cornel36.autominermod.menus.AutoMinerSettings;
import net.cornel36.autominermod.modes.MiningMode;
import net.minecraft.client.MinecraftClient;
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
        DONE
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
    private AutoMinerSettings.Mode currentMode = AutoMinerSettings.Mode.AREA;

    public void setMode(AutoMinerSettings.Mode mode) {
        this.currentMode = mode;
    }

    public AutoMinerTask(MinecraftClient client) {
        this.client = client;
    }

    public void start(BlockPos pos1, BlockPos pos2) {
        switch (currentMode) {
            case AREA:
                mode = new AreaMiningMode(client, pos1, pos2, Math.min(pos1.getY(), pos2.getY()), Math.max(pos1.getY(), pos2.getY()));
                break;
            case STRAIGHT:
                break; // TODO: Implement StraightMiningMode
            case MOB_GRINDER:
                break; // TODO: Implement MobGrinderMode
            default:
                mode = new AreaMiningMode(client, pos1, pos2, Math.min(pos1.getY(), pos2.getY()), Math.max(pos1.getY(), pos2.getY()));
                break;
        }
        mode.start(pos1, pos2);
        state = State.MINING;
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

//        public void startAreaMining (BlockPos pos1, BlockPos pos2){
//            this.areaPos1 = pos1;
//            this.areaPos2 = pos2;
//            this.minY = Math.min(pos1.getY(), pos2.getY());
//            this.startY = Math.max(pos1.getY(), pos2.getY());
//
//            this.mode = new AreaMiningMode(client, pos1, pos2, minY, startY);
//            this.state = State.MINING;
//        }

        public void stop () {
            this.state = State.IDLE;
            this.mode = null;
        }

        public boolean isRunning () {
            return state == State.MINING;
        }

        public boolean isDone () {
            return state == State.DONE;
        }

        public void markDone () {
            this.state = State.DONE;
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
