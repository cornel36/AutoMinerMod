package net.cornel36.autominermod.mixin;

import net.cornel36.autominermod.AutoMinerMod;
import net.cornel36.autominermod.AutoMinerTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects logic into the player's movement tick to enable automatic mining behavior.
 * Handles rotation, movement, and jumping toward the current mining target.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {

    /**
     * Called at the start of each tickMovement() on the client player.
     * If AutoMiner is active, it:
     * - Rotates the player to face the mining target.
     * - Simulates forward movement if target is not in reach.
     * - Simulates jumping if an obstacle is in front.
     */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo info) {
        if (!AutoMinerMod.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        AutoMinerTask task = AutoMinerMod.autoMinerTask;

        BlockPos target = task.getCurrentTarget();

        // If there is no target, stop moving forward.
        if (target == null) {
            client.options.forwardKey.setPressed(false);
            return;
        }

        // Calculate horizontal distance to the center of the target block
        Vec3d playerPos = player.getPos();
        Vec3d center = Vec3d.ofCenter(target);
        double dx = center.x - playerPos.x;
        double dz = center.z - playerPos.z;
        double distSq = dx * dx + dz * dz;

        // Face the target block by updating player yaw and pitch
        Vec3d dir = center.subtract(playerPos).normalize();
        float yaw = (float)Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float)Math.toDegrees(-Math.asin(dir.y));

        player.setYaw(yaw);
        player.setPitch(pitch);

        // Simulate holding "W" to move forward if target is not yet in reach
        client.options.forwardKey.setPressed(distSq > AutoMinerTask.REACH_DISTANCE_SQ);

        // Jump if there's an obstacle directly in front and space above it
        Vec3d look = player.getRotationVec(1.0F).normalize();
        BlockPos inFront = new BlockPos(
            (int)(playerPos.x + look.x),
            (int)(playerPos.y),
            (int)(playerPos.z + look.z)
        );
        BlockState frontBlock = client.world.getBlockState(inFront);

        boolean obstacleAhead = !frontBlock.isAir();
        boolean airAbove = client.world.getBlockState(inFront.up()).isAir();

        if (obstacleAhead && airAbove) {
            player.jump();
        }
    }
}
