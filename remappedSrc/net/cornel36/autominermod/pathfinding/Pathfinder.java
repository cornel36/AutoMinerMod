package net.cornel36.autominermod.pathfinding;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class Pathfinder {
    public static List<BlockPos> findPath(World world, BlockPos start, BlockPos goal) {
        Queue<BlockPos> openSet = new LinkedList<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();

        openSet.add(start);
        visited.add(start);

        while (!openSet.isEmpty()) {
            BlockPos current = openSet.poll();

            if (current.equals(goal)) {
                return reconstructPath(cameFrom, current);
            }

            for (BlockPos neighbor : getNeighbors(current)) {
                if (visited.contains(neighbor)) continue;

                if (isWalkable(world, neighbor)) {
                    openSet.add(neighbor);
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                }
            }
        }

        return new ArrayList<>();
    }

    private static List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> cameFrom, BlockPos current) {
        List<BlockPos> path = new ArrayList<>();
        while (cameFrom.containsKey(current)) {
            path.add(0, current);
            current = cameFrom.get(current);
        }
        return path;
    }

    private static boolean isWalkable(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == Blocks.AIR &&
                world.getBlockState(pos.down()).isOpaqueFullCube();
    }

    private static List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 4 cardinal directions + up (jump step)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) != 1) continue;

                BlockPos below = pos.add(dx, -1, dz);
                BlockPos flat = pos.add(dx, 0, dz);
                BlockPos up = pos.add(dx, 1, dz);

                neighbors.add(flat);
                neighbors.add(up); // for steps
            }
        }

        return neighbors;
    }
}
