package net.cornel36.autominermod.pathfinding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * A helper class that automates basic player movement toward a list of block positions.
 * Uses simulated key presses (forward, no jumping) and rotates the player to face targets.
 */
public class PlayerMovementHelper {

    private final MinecraftClient client = MinecraftClient.getInstance();

    private List<BlockPos> path = List.of();  // The current path the player should follow
    private int idx = 0;                      // Current index in the path

    /**
     * Sets a new movement path and resets the internal index.
     * @param p List of block positions representing the movement path.
     */
    public void setPath(List<BlockPos> p) {
        path = p;
        idx = 0;
    }

    /**
     * Stops the movement by releasing the forward key and jumping key.
     * Also sets the path index to the end.
     */
    public void stop() {
        client.options.forwardKey.setPressed(false);
        idx = path.size();
    }

    /**
     * Called every client tick to update movement state.
     * Rotates player to face the current target block and simulates forward movement.
     */
    public void tick() {
        // If path is exhausted, do nothing
        if (idx >= path.size()) return;

        BlockPos t = path.get(idx);
        Vec3d pos = client.player.getPos();

        // If close enough to the target, proceed to the next point
        if(pos.squaredDistanceTo(Vec3d.ofCenter(t)) < 1.5) {
         idx++;
          return;
        }

        // Face the current target and move forward
        face(t);
        client.options.forwardKey.setPressed(true);
        client.options.jumpKey.setPressed(false); // Disable jumping for now
    }

    /**
     * Rotates the player's yaw to face a specific block position.
     * @param target The block to face.
     */
    private void face(BlockPos t) {
        Vec3d p = client.player.getPos();
        Vec3d tv = Vec3d.ofCenter(t);

        // Direction vector from player to target
        Vec3d d = tv.subtract(p).normalize();

        // Calculate yaw (rotation around vertical axis)
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x,d.z));

        client.player.setYaw(yaw);
    }
}