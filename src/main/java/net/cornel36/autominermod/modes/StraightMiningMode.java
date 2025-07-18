package net.cornel36.autominermod.modes;

import net.cornel36.autominermod.AutoMinerTask;
import net.cornel36.autominermod.menus.AutoMinerSettings;
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

import java.util.ArrayList;
import java.util.List;

public class StraightMiningMode implements MiningMode {

    private final MinecraftClient client;
    private AutoMinerTask.State state = AutoMinerTask.State.IDLE;

    private BlockPos lastMinedPos;
    private int breakingTicks = 0;
    private int postBreakDelayTicks = 0;
    private boolean waitingForBreakCheck = false;

    private BlockPos currentTargetBlock = null;

    private long startTimeMillis = 0;
    private int blocksMined = 0;

    public StraightMiningMode(MinecraftClient client) {
        this.client = client;
    }

    @Override
    public void start(BlockPos pos1, BlockPos pos2) {
        stop();

        this.startTimeMillis = System.currentTimeMillis();
        this.blocksMined = 0;
        this.lastMinedPos = null;
        this.breakingTicks = 0;
        this.postBreakDelayTicks = 0;
        this.waitingForBreakCheck = false;
        this.state = AutoMinerTask.State.MINING;
    }

    @Override
    public void stop() {
        this.state = AutoMinerTask.State.IDLE;
        this.currentTargetBlock = null;
        this.breakingTicks = 0;
        this.postBreakDelayTicks = 0;
        this.waitingForBreakCheck = false;
        releaseAllMovementKeys();

    }

    @Override
    public boolean isRunning() {
        return this.state == AutoMinerTask.State.MINING;
    }

    @Override
    public void tick() {
        if (client.player == null || client.world == null) return;
        if (state != AutoMinerTask.State.MINING) return;

        handleMiningTick();
    }

    @Override
    public List<String> getDebugText() {
        List<String> lines = new ArrayList<>();
        lines.add("Mode: Straight Mining");
        lines.add("State: " + state.name());
        lines.add("Blocks Mined: " + blocksMined);
        lines.add(String.format("Elapsed: %.1f sec", getElapsedTimeMillis() / 1000.0));
        lines.add("Move Forward Enabled: " + AutoMinerSettings.isStraightMiningWalkForward());
        if (lastMinedPos != null) {
            lines.add("Last Target: " + lastMinedPos.toShortString());
        }
        lines.add("Breaking Ticks: " + breakingTicks);
        lines.add("Current Target: " + (currentTargetBlock != null ? currentTargetBlock.toShortString() : "None"));
        return lines;
    }

    @Override
    public boolean isMoveForwardEnabled() {
        return AutoMinerSettings.isStraightMiningWalkForward();
    }

    @Override
    public BlockPos getCurrentTarget() {
        if (client.player == null || client.world == null) return null;
        return client.player.getBlockPos().offset(client.player.getHorizontalFacing()).up();
    }

    @Override
    public int getCurrentY() {
        return client.player != null ? client.player.getBlockY() : 0;
    }

    //in this mode y doesnt matter cause we're not going up nor down
    @Override
    public int getMinY() {
        return getCurrentY();
    }

    @Override
    public int getTotalBlockCount() {
        return blocksMined;
    }

    @Override
    public int getRemainingBlocks() {
        return 0;
    }

    private void handleMiningTick() {
        if (postBreakDelayTicks > 0) {
            postBreakDelayTicks--;
            releaseAllMovementKeys();
            return;
        }

        if (waitingForBreakCheck) {
            checkBreakSuccess();
            return;
        }

        boolean blocksAhead = mineFrontBlocks();

        if (AutoMinerSettings.isStraightMiningWalkForward()) {
            BlockPos playerNextPos = client.player.getBlockPos().offset(client.player.getHorizontalFacing());
            if (!isSolidGround(playerNextPos.down())) {
                if (placeBlockBelowIfNeeded(playerNextPos)) {
                    releaseAllMovementKeys();
                    return;
                }
            }
        }

        if (!blocksAhead && AutoMinerSettings.isStraightMiningWalkForward()) {

            moveForward();
        } else {
            releaseAllMovementKeys();
        }
    }

    private boolean mineFrontBlocks() {
        if (client.player == null || client.world == null) return false;

        BlockPos playerPos = client.player.getBlockPos();
        Direction facing = client.player.getHorizontalFacing();

        BlockPos targetLegs = playerPos.offset(facing);
        BlockPos targetHead = playerPos.offset(facing).up();

        if (currentTargetBlock != null && !client.world.isAir(currentTargetBlock) && client.world.getBlockState(currentTargetBlock).getBlock() != Blocks.BEDROCK) {
            mineBlock(currentTargetBlock);
            return true;
        } else {
            currentTargetBlock = null;
            breakingTicks = 0;
        }

        if (currentTargetBlock == null) {
            if (!client.world.isAir(targetLegs) && client.world.getBlockState(targetLegs).getBlock() != Blocks.BEDROCK) {
                currentTargetBlock = targetLegs;
            } else if (!client.world.isAir(targetHead) && client.world.getBlockState(targetHead).getBlock() != Blocks.BEDROCK) {
                currentTargetBlock = targetHead;
            }
        }

        if (currentTargetBlock != null) {
            mineBlock(currentTargetBlock);
            return true;
        }

        return false;
    }

    private void mineBlock(BlockPos target) {
        if (client.player == null || client.world == null) return;

        BlockState state = client.world.getBlockState(target);
        if (state.isAir()) {
            postBreakDelayTicks = 4;
            waitingForBreakCheck = true;
            lastMinedPos = target;
            return;
        }

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
        } else {
            breakingTicks = 0;
        }

        if (client.world.isAir(target)) {
            postBreakDelayTicks = 4;
            waitingForBreakCheck = true;
            lastMinedPos = target;
            return;
        }

        if (breakingTicks > 100) {
            System.out.println("Skipping block due to timeout: " + target);
            postBreakDelayTicks = 4;
            waitingForBreakCheck = true;
            lastMinedPos = target;
            currentTargetBlock = null;
            return;
        }
    }

    private void checkBreakSuccess() {
        if (lastMinedPos == null || client.world == null) return;

        if (client.world.getBlockState(lastMinedPos).isAir()) {
            blocksMined++;
        }
        breakingTicks = 0;
        waitingForBreakCheck = false;
        lastMinedPos = null;
        currentTargetBlock = null;
    }

    private boolean placeBlockBelowIfNeeded(BlockPos dest) {
        if (client.player == null || client.world == null) return false;

        BlockPos below = dest.down();

        if (!isSolidGround(below)) {
            int slot = findPlaceableBlockInHotbar();
            if (slot == -1) {
                System.out.println("No placeable blocks in hotbar!");
                return false;
            }

            int previousSlot = client.player.getInventory().getSelectedSlot();
            client.player.getInventory().setSelectedSlot(slot);
            lookAt(below);

            Vec3d hitVec = Vec3d.ofCenter(below);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, below, false);

            ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);

            client.player.getInventory().setSelectedSlot(previousSlot);

            if (result == ActionResult.SUCCESS || result == ActionResult.CONSUME) {
                client.player.swingHand(Hand.MAIN_HAND);
                postBreakDelayTicks = 2;
                return true;
            }
        }
        return false;
    }

    private void moveForward() {
        // operated by mixin
    }

    private void releaseAllMovementKeys() {
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
    }

    private void lookAt(BlockPos target) {
        if (client.player == null) return;

        Vec3d eyePos = client.player.getCameraPosVec(1.0F);
        Vec3d targetVec = Vec3d.ofCenter(target);
        Vec3d delta = targetVec.subtract(eyePos);

        double distXZ = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, distXZ));

        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
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
        if (client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();

                if (block != Blocks.AIR && block != Blocks.GRAVEL && block != Blocks.SAND && block.getDefaultState().isFullCube(client.world, BlockPos.ORIGIN)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isSolidGround(BlockPos pos) {
        if (client.world == null) return false;
        BlockState state = client.world.getBlockState(pos);
        return state.isFullCube(client.world, pos)
                && state.getBlock() != Blocks.AIR
                && state.getBlock() != Blocks.GRAVEL
                && state.getBlock() != Blocks.SAND;
    }

    private Direction getPreferredDirection(BlockPos target) {
        if (client.player == null) return Direction.UP;

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

    private int getBlocksMined() {
        return blocksMined;
    }

    private long getElapsedTimeMillis() {
        return System.currentTimeMillis() - startTimeMillis;
    }
}
