package net.cornel36.autominermod.modes;

import net.cornel36.autominermod.AutoMinerTask;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AreaMiningMode implements MiningMode {

    // Constants
    public static final double REACH_DISTANCE_SQ = 25.0;
    public static final double MAX_BREAK_DISTANCE_SQ = 25.0;

    // Fields
    private final MinecraftClient client;
    private Queue<BlockPos> queue = new LinkedList<>();

    private int estimatedTimeLeftSeconds = 0;
    private long lastSecondUpdateTime = 0;

    private BlockPos currentTarget = null;
    private long startTimeMillis = 0;
    private int totalBlockCount = 0;
    private int blocksMinedSoFar = 0;

    private int postBreakDelayTicks = 0;
    private boolean waitingForBreakCheck = false;

    private int currentY;
    private int minY;
    private int breakingTicks = 0;
    private AutoMinerTask.State state = AutoMinerTask.State.IDLE;

    private int refreshTickCounter = 0;

    private BlockPos areaPos1;
    private BlockPos areaPos2;

    // Constructor
    public AreaMiningMode(MinecraftClient client, BlockPos pos1, BlockPos pos2, int minY, int startY) {
        this.client = client;
        this.areaPos1 = pos1;
        this.areaPos2 = pos2;
        this.minY = minY;
        this.currentY = startY;

        this.queue = new LinkedList<>();
        this.refreshTickCounter = 0;
        this.totalBlockCount = 0;
        this.blocksMinedSoFar = 0;
        this.breakingTicks = 0;
        this.postBreakDelayTicks = 0;
        this.waitingForBreakCheck = false;
    }

    // === Public API ===

    /**
     * Starts the AutoMiner with the selected area defined by two corner positions.
     * Initializes internal state and block queue.
     */
    public void start(BlockPos pos1, BlockPos pos2) {
        stop();

        this.areaPos1 = pos1;
        this.areaPos2 = pos2;

        this.currentY = Math.max(pos1.getY(), pos2.getY());
        this.minY = Math.min(pos1.getY(), pos2.getY());

        this.currentTarget = null;
        this.breakingTicks = 0;
        this.postBreakDelayTicks = 0;
        this.waitingForBreakCheck = false;
        this.blocksMinedSoFar = 0;

        queue.clear();
        List<BlockPos> firstLayer = selectBlocksInLayer(areaPos1, areaPos2, currentY);
        queue.addAll(firstLayer);

        int estimatedTicks = 0;
        for (int y = minY; y <= currentY; y++) {
            List<BlockPos> layer = selectBlocksInLayer(areaPos1, areaPos2, y);
            for (BlockPos p : layer) {
                estimatedTicks += getRequiredBreakTicks(p);
            }
        }
        this.estimatedTimeLeftSeconds = estimatedTicks / 20;
        this.lastSecondUpdateTime = System.currentTimeMillis();

        this.totalBlockCount = countTotalNonAirBlocksInArea(areaPos1, areaPos2, minY, currentY);
        this.startTimeMillis = System.currentTimeMillis();

        this.state = AutoMinerTask.State.MINING;
    }

    /**
     * Stops the AutoMiner and resets internal state.
     * Also releases any simulated key presses.
     */
    public void stop() {
        queue.clear();
        currentTarget = null;
        breakingTicks = 0;
        postBreakDelayTicks = 0;
        waitingForBreakCheck = false;
        state = AutoMinerTask.State.IDLE;
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.useKey.setPressed(false);
    }

    /**
     * Returns whether the AutoMiner is currently running.
     */
    public boolean isRunning() {
        return state != AutoMinerTask.State.IDLE && state != AutoMinerTask.State.DONE;
    }

    public BlockPos getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Called every client tick. Performs mining logic if the miner is running.
     */
    public void tick() {
        if (client.player == null || client.world == null) return;
        if (!isRunning()) return;

        handleMiningTick();
        client.options.useKey.setPressed(false);

        // refresh queue every 10 ticks
        refreshTickCounter++;
        if (refreshTickCounter >= 10) {
            refreshCurrentLayerQueue();
            refreshTickCounter = 0;
        }

        long now = System.currentTimeMillis();
        if (now - lastSecondUpdateTime >= 1000) {
            lastSecondUpdateTime = now;
            if (estimatedTimeLeftSeconds > 0) {
                estimatedTimeLeftSeconds--;
            }
        }
    }

    /**
     * Returns a list of debug lines to display in the HUD overlay.
     * Includes state info, progress, time elapsed, etc.
     */
    public List<String> getDebugText() {
        List<String> lines = new ArrayList<>();
        lines.add("State: " + state.name());
        lines.add("Current Layer Y: " + currentY);
        if (currentTarget != null) {
            lines.add("Current Target: " + currentTarget.toShortString());
        }
        lines.add("Queue: " + queue.size() + " blocks");
        lines.add("Breaking Ticks: " + breakingTicks);

        int remaining = queue.size();
        if (currentTarget != null) remaining += 1;

        lines.add("Blocks left: " + remaining + " / " + totalBlockCount);

        if (totalBlockCount > 0) {
            int percent = (int) ((blocksMinedSoFar * 100.0) / totalBlockCount);
            lines.add(String.format("Progress: %d / %d blocks (%d%%)", blocksMinedSoFar, totalBlockCount, percent));
        } else {
            lines.add("Progress: 0 / 0 blocks (0%)");
        }

        long now = System.currentTimeMillis();
        double secondsElapsed = (now - startTimeMillis) / 1000.0;
        lines.add(String.format("Elapsed: %.1f sec", secondsElapsed));
        lines.add(String.format("Estimated time left: %d sec", estimatedTimeLeftSeconds));

        return lines;
    }

// === Internal logic ===

    /**
     * Selects the next target block to mine and manages movement/mining attempts.
     */
    private void handleMiningTick() {
        if (postBreakDelayTicks > 0) {
            postBreakDelayTicks--;
            return;
        }

        // Wait for block to disappear after breaking
        if (waitingForBreakCheck) {
            checkBreakSuccess();
            return;
        }

        // Select next target
        if (currentTarget == null || client.world.isAir(currentTarget)) {
            currentTarget = pollNext();
            breakingTicks = 0;
        }

        // No more targets in layer? Go to next layer
        if (currentTarget == null) {
            if (!replenishQueueOrMoveDown()) {
                state = AutoMinerTask.State.DONE;
            }
            return;
        }

        // Move toward target
        BlockPos playerPos = client.player.getBlockPos();
        BlockPos adjacent = findAdjacentMiningPosition(currentTarget, playerPos);

        if (!playerPos.equals(adjacent)) {
            walkTo(adjacent);
            return;
        }

        // In position → mine
        releaseAllMovementKeys();
        lookAt(currentTarget);

        double distSq = client.player.getPos().squaredDistanceTo(Vec3d.ofCenter(currentTarget));
        if (distSq > MAX_BREAK_DISTANCE_SQ) {
            walkTo(adjacent);
            return;
        }

        boolean broken = mineBlock(currentTarget);

        if (broken && !waitingForBreakCheck) {
            currentTarget = null;
            breakingTicks = 0;
            blocksMinedSoFar++;
        } else if (!broken) {
            breakingTicks++;
        }
    }

    /**
     * Called after we try to break a block — checks if the block is now air.
     */
    private void checkBreakSuccess() {
        if (currentTarget == null || client.world == null) return;

        if (client.world.getBlockState(currentTarget).isAir()) {
            currentTarget = null;
            blocksMinedSoFar++;
            refreshCurrentLayerQueue();
        }
        breakingTicks = 0;
        waitingForBreakCheck = false;
    }

    /**
     * Attempts to mine the given target block.
     *
     * @param target The block to mine.
     * @return true if the block was broken, false if still in progress or failed.
     */
    private boolean mineBlock(BlockPos target) {
        BlockState state = client.world.getBlockState(target);
        if (state.isAir()) return true;

        lookAt(target);
        selectBestTool(target);
        Direction direction = getPreferredDirection(target);

        if (breakingTicks == 0) {
            client.interactionManager.attackBlock(target, direction);
        }

        boolean continueBreaking = client.interactionManager.updateBlockBreakingProgress(target, direction);
        client.player.swingHand(Hand.MAIN_HAND);

        if (continueBreaking) {
            breakingTicks++;
        }

        if (client.world.getBlockState(target).isAir()) {
            postBreakDelayTicks = 4;
            waitingForBreakCheck = true;
            return true;
        }

        if (breakingTicks > 100) {
            System.out.println("Skipping unreachable block: " + target);
            return true;
        }

        return false;
    }

    /**
     * Polls the next non-air block from the queue.
     */
    private BlockPos pollNext() {
        while (!queue.isEmpty()) {
            BlockPos next = queue.poll();
            if (!client.world.isAir(next)) {
                return next;
            }
        }
        return null;
    }

    /**
     * Replenishes the block queue or moves to the next layer.
     *
     * @return true if queue was refilled, false if no more layers
     */
    private boolean replenishQueueOrMoveDown() {
        List<BlockPos> remaining = selectBlocksInLayer(areaPos1, areaPos2, currentY);
        remaining.removeIf(pos -> client.world.isAir(pos));

        if (!remaining.isEmpty()) {
            queue.clear();
            queue.addAll(remaining);
            return true;
        }

        currentY--;
        if (currentY < minY) return false;

        queue.clear();
        queue.addAll(selectBlocksInLayer(areaPos1, areaPos2, currentY));
        currentTarget = null;
        state = AutoMinerTask.State.MINING;
        return true;
    }


    // === Player movement ===
    private void walkTo(BlockPos dest) {
        releaseAllMovementKeys();

        BlockPos playerPos = client.player.getBlockPos();
        int dx = dest.getX() - playerPos.getX();
        int dz = dest.getZ() - playerPos.getZ();

        BlockPos ground = dest.down();

        if (!isSolidGround(ground)) {
            boolean placed = placeBlockBelowIfNeeded(dest);
            if (!placed || client.world.isAir(ground)) {
                releaseAllMovementKeys();
                return;
            }
        }

        if (dx > 0) client.options.rightKey.setPressed(true);
        if (dx < 0) client.options.leftKey.setPressed(true);
        if (dz > 0) client.options.backKey.setPressed(true);
        if (dz < 0) client.options.forwardKey.setPressed(true);
    }

    private void releaseAllMovementKeys() {
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
    }

    /**
     * Returns the optimal adjacent block from which the player can mine the target.
     * Defaults to player's current position if none is found.
     */
    private BlockPos findAdjacentMiningPosition(BlockPos target, BlockPos player) {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos adjacent = target.offset(dir);
            if (player.equals(adjacent)) {
                return adjacent;
            }
        }
        return player;
    }

    private boolean placeBlockBelowIfNeeded(BlockPos dest) {
        BlockPos below = dest.down();

        if (below.getY() < currentY && client.world.isAir(below)) {
            int slot = findPlaceableBlockInHotbar();
            if (slot == -1) return false;

            client.player.getInventory().setSelectedSlot(slot);
            lookAt(below);

            Vec3d hitVec = Vec3d.ofCenter(below);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, below, false);

            ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);

            if (result == ActionResult.SUCCESS || result == ActionResult.CONSUME) {
                client.player.swingHand(Hand.MAIN_HAND);
                postBreakDelayTicks = 2;
                return true;
            }
        }

        return !client.world.isAir(below);
    }

    private void lookAt(BlockPos target) {
        Vec3d eyePos = client.player.getCameraPosVec(1.0F);
        Vec3d targetVec = Vec3d.ofCenter(target);
        Vec3d delta = targetVec.subtract(eyePos);

        double distXZ = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, distXZ));

        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }

    private Direction getPreferredDirection(BlockPos target) {
        Vec3d eyePos = client.player.getCameraPosVec(1.0F);
        Vec3d targetVec = Vec3d.ofCenter(target);
        Vec3d delta = targetVec.subtract(eyePos).normalize();

        double absX = Math.abs(delta.x);
        double absY = Math.abs(delta.y);
        double absZ = Math.abs(delta.z);

        if (absY >= absX && absY >= absZ) {
            return delta.y > 0 ? Direction.UP : Direction.DOWN;
        } else if (absX >= absZ) {
            return delta.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return delta.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private void selectBestTool(BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        float bestSpeed = 0.0f;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            var stack = client.player.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            client.player.getInventory().setSelectedSlot(bestSlot);
        }
    }

    private int findPlaceableBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            var stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();

                if (block != Blocks.AIR && block != Blocks.GRAVEL && block != Blocks.SAND) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns a list of all non-air, non-bedrock blocks in the given Y layer.
     * Traverses layer in zigzag pattern for more efficient mining.
     */
    private List<BlockPos> selectBlocksInLayer(BlockPos pos1, BlockPos pos2, int y) {
        List<BlockPos> result = new ArrayList<>();
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        boolean reverseX = false;
        for (int z = minZ; z <= maxZ; z++) {
            if (reverseX) {
                for (int x = maxX; x >= minX; x--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
                        result.add(pos);
                    }
                }
            } else {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
                        result.add(pos);
                    }
                }
            }
            reverseX = !reverseX;
        }

        return result;
    }

    private int countTotalNonAirBlocksInArea(BlockPos p1, BlockPos p2, int yMin, int yMax) {
        int count = 0;
        for (int y = yMin; y <= yMax; y++) {
            List<BlockPos> layer = selectBlocksInLayer(p1, p2, y);
            count += layer.size();
        }
        return count;
    }

    private int getRequiredBreakTicks(BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        float hardness = state.getHardness(client.world, pos);
        if (hardness < 0) return 0;
        return (int) (hardness * 100);
    }

    private void refreshCurrentLayerQueue() {
        if (areaPos1 == null || areaPos2 == null) return;

        List<BlockPos> currentLayer = selectBlocksInLayer(areaPos1, areaPos2, currentY);
        currentLayer.removeIf(pos -> client.world.isAir(pos));

        queue.removeIf(pos -> !currentLayer.contains(pos));

        int added = 0;
        for (BlockPos pos : currentLayer) {
            if (!queue.contains(pos) && !Objects.equals(currentTarget, pos)) {
                queue.add(pos);
                added++;
            }
        }

        if (added > 0) {
            totalBlockCount += added;
        }
    }

    private boolean isSolidGround(BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        return state.isFullCube(client.world, pos)
                && state.getBlock() != Blocks.AIR
                && state.getBlock() != Blocks.GRAVEL
                && state.getBlock() != Blocks.SAND;
    }

// === HUD getters ===
    public int getCurrentY() {
        return currentY;
    }

    public int getMinY() {
        return minY;
    }

    public int getTotalBlockCount() {
        return totalBlockCount;
    }

    public int getRemainingBlocks() {
        return totalBlockCount - blocksMinedSoFar;
    }
}