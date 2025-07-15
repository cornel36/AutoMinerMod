package net.cornel36.autominermod.pathfinding;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * A Minecraft-specific A* pathfinding implementation that accounts for
 * player walkability (e.g., jumping up, falling down, standing space).
 */
public class PathfinderAStar {

    /**
     * Internal node used in the A* search algorithm.
     * Holds position, parent reference, movement cost (g), and estimated cost to goal (f).
     */
    private record Node(BlockPos pos, Node parent, double g, double f) {}

    /**
     * Finds a path from {@code start} to {@code goal} within the given 3D bounding box.
     *
     * @param world The Minecraft world for block checks.
     * @param start The start block position.
     * @param goal The goal block position.
     * @param min The minimum bound (inclusive) of the allowed area.
     * @param max The maximum bound (inclusive) of the allowed area.
     * @return A list of block positions forming the path, or an empty list if no path found.
     */
    public static List<BlockPos> findPath(World world, BlockPos start, BlockPos goal, BlockPos min, BlockPos max) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, Node> all = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        Node startNode = new Node(start, null, 0, heuristic(start, goal));
        open.add(startNode);
        all.put(start, startNode);

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (current.pos.equals(goal)) {
                return reconstruct(current);
            }

            closed.add(current.pos);

            for (Neighbor neighbor : getNeighbors(world, current.pos, min, max)) {
                if (closed.contains(neighbor.pos)) continue;

                double newG = current.g + neighbor.cost;
                Node existing = all.get(neighbor.pos);

                // If it's a better or new path to neighbor
                if (existing == null || newG < existing.g) {
                    double newF = newG + heuristic(neighbor.pos, goal);
                    Node newNode = new Node(neighbor.pos, current, newG, newF);
                    open.add(newNode);
                    all.put(neighbor.pos, newNode);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    /**
     * Reconstructs the path by walking backwards through parent nodes.
     */
    private static List<BlockPos> reconstruct(Node node) {
        LinkedList<BlockPos> path = new LinkedList<>();
        while (node != null) {
            path.addFirst(node.pos);
            node = node.parent;
        }
        return path;
    }

    /**
     * Simple Manhattan distance heuristic.
     */
    private static double heuristic(BlockPos a, BlockPos b) {
        return a.getManhattanDistance(b);
    }

    /**
     * Represents a walkable neighbor candidate and its movement cost.
     */
    private static class Neighbor {
        BlockPos pos;
        double cost;

        Neighbor(BlockPos pos, double cost) {
            this.pos = pos;
            this.cost = cost;
        }
    }

    /**
     * Determines whether a transition between two blocks is physically walkable by a player.
     */
    private static boolean isWalkableTransition(World world, BlockPos from, BlockPos to) {
        int dy = to.getY() - from.getY();

        if (dy > 0) {
            // Jumping up: both upper blocks must be empty
            return world.getBlockState(from.up()).isAir() && world.getBlockState(to.up()).isAir();
        }

        if (dy < 0) {
            // Falling down: must be able to stand at destination
            return canStand(world, to);
        }

        return true; // Same level
    }

    /**
     * Generates walkable neighbor blocks from the current position.
     * Considers 4 cardinal directions and movement up/down by 3 blocks.
     */
    private static List<Neighbor> getNeighbors(World world, BlockPos pos, BlockPos min, BlockPos max) {
        List<Neighbor> neighbors = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                // Only cardinal directions (N, S, E, W)
                if (Math.abs(dx) + Math.abs(dz) != 1) continue;

                for (int dy = -3; dy <= 1; dy++) {
                    BlockPos np = pos.add(dx, dy, dz);

                    if (!isWithin(np, min, max)) continue;
                    if (!canStand(world, np)) continue;
                    if (!isWalkableTransition(world, pos, np)) continue;

                    double cost = 1 + Math.abs(dy); // Higher cost for vertical movement
                    neighbors.add(new Neighbor(np, cost));
                }
            }
        }

        return neighbors;
    }

    /**
     * Checks if a position is within the allowed bounding box.
     */
    private static boolean isWithin(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * Checks if the player can stand at the given block:
     * - Air at the position and one above
     * - Solid ground below
     */
    private static boolean canStand(World world, BlockPos pos) {
        return world.getBlockState(pos).isAir()
            && world.getBlockState(pos.up()).isAir()
            && world.getBlockState(pos.down()).isOpaque();
    }
}
