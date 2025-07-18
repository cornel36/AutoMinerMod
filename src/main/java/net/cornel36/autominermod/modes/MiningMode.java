package net.cornel36.autominermod.modes;

import net.minecraft.util.math.BlockPos;
import java.util.List;

public interface MiningMode {
    void start(BlockPos pos1, BlockPos pos2);

    boolean isRunning();

    void tick();
    void stop();
    BlockPos getCurrentTarget();
    List<String> getDebugText();

    int getCurrentY();
    int getMinY();
    int getTotalBlockCount();
    int getRemainingBlocks();

    boolean isMoveForwardEnabled();
}
