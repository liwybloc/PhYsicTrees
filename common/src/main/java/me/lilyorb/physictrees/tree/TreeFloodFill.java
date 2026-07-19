package me.lilyorb.physictrees.tree;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

@UtilityClass
public final class TreeFloodFill {
    private static final int MAX_LOGS = 256;
    private static final int MAX_LEAVES = 2048;
    private static final int MAX_ATTACHED_BLOCKS = 256;
    private static final int LEAF_PADDING = 7;
    private static final int MAX_LEAF_CONNECTION_DISTANCE = LEAF_PADDING;
    private static final Direction[] CARDINAL_DIRECTIONS = Direction.values();
    private static final BlockPos[] LOG_NEIGHBORS = buildNeighbors();

    public static TreeResult findTree(final BlockGetter level, final BlockPos start, final BlockPos ignoredBrokenPos) {
        final BlockState startState = level.getBlockState(start);
        if (!TreeUtil.isLog(startState)) {
            return null;
        }

        final Set<BlockPos> connectedLogs = collectConnectedLogs(level, start, ignoredBrokenPos, Set.of());
        if (connectedLogs.isEmpty()) {
            return null;
        }
        final TreeResult result = new TreeResult();
        if (!hasValidFallingConnection(level, start, connectedLogs)) {
            return null;
        }
        result.markGrounded();
        final Set<BlockPos> protectedLeaves = new HashSet<>();

        int minX = start.getX();
        int minY = start.getY();
        int minZ = start.getZ();
        int maxX = start.getX();
        int maxY = start.getY();
        int maxZ = start.getZ();

        for (final BlockPos current : connectedLogs) {
            if (current.getY() < start.getY()) {
                continue;
            }

            result.addLog(current, blockMass(level, current));
            minX = Math.min(minX, current.getX());
            minY = Math.min(minY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxX = Math.max(maxX, current.getX());
            maxY = Math.max(maxY, current.getY());
            maxZ = Math.max(maxZ, current.getZ());
        }

        if (result.logs().isEmpty() || result.logs().size() > MAX_LOGS) {
            return null;
        }

        protectNearbyForeignTreeLeaves(level, result.logs(), connectedLogs, protectedLeaves, minX, minY, minZ, maxX, maxY, maxZ);
        collectLeaves(level, result, protectedLeaves, start.getY(), minX, minY, minZ, maxX, maxY, maxZ);
        pruneDistantAttachedLeaves(level, result, connectedLogs);
        collectAttachedBlocks(level, result, start.getY());
        return result.isValid() ? result : null;
    }

    private static boolean hasValidFallingConnection(final BlockGetter level, final BlockPos start, final Set<BlockPos> connectedLogs) {
        boolean hasGroundedBase = false;
        final Set<BlockPos> visited = new HashSet<>();

        for (final BlockPos log : connectedLogs) {
            if (log.equals(start) || visited.contains(log)) {
                continue;
            }

            final LogComponent component = collectLogComponent(level, log, start, connectedLogs);
            visited.addAll(component.logs());
            if (component.grounded()) {
                hasGroundedBase = true;
                if (component.reachesCutHeight(start.getY())) {
                    return false;
                }
            }
        }

        return hasGroundedBase || isGroundedLog(level, start);
    }

    private static LogComponent collectLogComponent(final BlockGetter level, final BlockPos start, final BlockPos ignoredBrokenPos, final Set<BlockPos> connectedLogs) {
        final Set<BlockPos> visited = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        final BlockPos immutableStart = start.immutable();
        queue.add(immutableStart);
        visited.add(immutableStart);
        boolean grounded = false;
        int highestY = start.getY();

        while (!queue.isEmpty()) {
            final BlockPos current = queue.removeFirst();
            grounded = grounded || isGroundedLog(level, current);
            highestY = Math.max(highestY, current.getY());
            final BlockState currentState = level.getBlockState(current);
            for (final BlockPos offset : LOG_NEIGHBORS) {
                final BlockPos next = current.offset(offset);
                if (isIgnored(next, ignoredBrokenPos) || !connectedLogs.contains(next) || visited.contains(next)) {
                    continue;
                }

                final BlockState nextState = level.getBlockState(next);
                if (canSpreadLog(current, next, currentState, nextState)) {
                    final BlockPos immutableNext = next.immutable();
                    visited.add(immutableNext);
                    queue.add(immutableNext);
                }
            }
        }

        return new LogComponent(visited, grounded, highestY);
    }

    private static boolean isGroundedLog(final BlockGetter level, final BlockPos log) {
        final BlockState below = level.getBlockState(log.below());
        return TreeUtil.isRoot(below) || TreeUtil.canBeRoot(below);
    }

    private static Set<BlockPos> collectConnectedLogs(final BlockGetter level, final BlockPos start, final BlockPos ignoredBrokenPos, final Set<BlockPos> excludedLogs) {
        final Set<BlockPos> visited = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        final BlockPos immutableStart = start.immutable();
        queue.add(immutableStart);
        visited.add(immutableStart);

        while (!queue.isEmpty()) {
            if (visited.size() > MAX_LOGS) {
                return Set.of();
            }

            final BlockPos current = queue.removeFirst();
            final BlockState currentState = level.getBlockState(current);
            if (isIgnored(current, ignoredBrokenPos) || !TreeUtil.isLog(currentState)) {
                continue;
            }

            for (final BlockPos offset : LOG_NEIGHBORS) {
                final BlockPos next = current.offset(offset);
                if (excludedLogs.contains(next) || isIgnored(next, ignoredBrokenPos) || visited.contains(next)) {
                    continue;
                }

                final BlockState nextState = level.getBlockState(next);
                if (canSpreadLog(current, next, currentState, nextState)) {
                    final BlockPos immutableNext = next.immutable();
                    if (visited.add(immutableNext)) {
                        queue.add(immutableNext);
                    }
                }
            }
        }

        return visited;
    }

    private static boolean canSpreadLog(final BlockPos fromPos, final BlockPos toPos, final BlockState fromState, final BlockState toState) {
        if (!TreeUtil.isLog(fromState) || !TreeUtil.isLog(toState)) {
            return false;
        }

        if (TreeUtil.isSameLogType(fromState, toState)) {
            return true;
        }

        final BlockPos relative = toPos.subtract(fromPos);
        final boolean hasVerticalStep = relative.getY() != 0;
        final boolean hasHorizontalStep = relative.getX() != 0 || relative.getZ() != 0;
        if (hasVerticalStep && hasHorizontalStep) {
            return true;
        }

        final Direction direction = Direction.getNearest(relative.getX(), relative.getY(), relative.getZ());
        return TreeUtil.getLogAxis(fromState) == direction.getAxis() && TreeUtil.getLogAxis(toState) == direction.getAxis();
    }

    private static Set<BlockPos> findGroundedForeignLogs(final BlockGetter level, final BlockPos start, final Set<BlockPos> connectedToMinedBlock) {
        final Set<BlockPos> visited = collectConnectedLogs(level, start, null, connectedToMinedBlock);
        if (visited.isEmpty()) {
            return Set.of();
        }

        boolean connectedToGround = false;

        for (final BlockPos current : visited) {
            final BlockState below = level.getBlockState(current.below());
            if (TreeUtil.isRoot(below) || TreeUtil.canBeRoot(below)) {
                connectedToGround = true;
            }
        }

        return connectedToGround ? visited : Set.of();
    }

    private static void protectLeavesNear(final BlockGetter level, final Set<BlockPos> mainLogs, final Set<BlockPos> protectedLogs, final Set<BlockPos> protectedLeaves, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        final ClosestLogs closestLogs = findClosestLogs(mainLogs, protectedLogs);
        if (closestLogs == null) {
            return;
        }

        final int protectionRange = Math.max(0, closestLogs.distance / 2);
        final int toMainX = Integer.compare(closestLogs.mainLog.getX() - closestLogs.foreignLog.getX(), 0);
        final int toMainZ = Integer.compare(closestLogs.mainLog.getZ() - closestLogs.foreignLog.getZ(), 0);
        if (protectionRange <= 0 && toMainX == 0 && toMainZ == 0) {
            return;
        }

        final Set<BlockPos> visitedLeaves = new HashSet<>();
        final ArrayDeque<LeafSearchNode> queue = new ArrayDeque<>();

        for (final BlockPos logPos : protectedLogs) {
            checkProtectedLeafNeighbors(level, minX, minY, minZ, maxX, maxY, maxZ, visitedLeaves, queue, logPos, 1);
        }

        while (!queue.isEmpty()) {
            final LeafSearchNode current = queue.removeFirst();
            final BlockPos nearestForeignLog = nearestLog(current.pos, protectedLogs);
            if (nearestForeignLog == null) {
                continue;
            }

            if (current.distance <= protectionRange || isFacingAwayFromMain(current.pos, nearestForeignLog, toMainX, toMainZ)) {
                protectedLeaves.add(current.pos);
            }

            if (current.distance >= LEAF_PADDING) {
                continue;
            }

            checkProtectedLeafNeighbors(level, minX, minY, minZ, maxX, maxY, maxZ, visitedLeaves, queue, current.pos, current.distance + 1);
        }
    }

    private static void checkProtectedLeafNeighbors(final BlockGetter level, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ, final Set<BlockPos> visitedLeaves, final ArrayDeque<LeafSearchNode> queue, final BlockPos sourcePos, final int nextDistance) {
        for (final Direction direction : CARDINAL_DIRECTIONS) {
            final BlockPos leafPos = sourcePos.relative(direction);
            if (isWithinLeafBounds(leafPos, minX, minY, minZ, maxX, maxY, maxZ)
                    && visitedLeaves.add(leafPos.immutable())
                    && isCollectableLeaf(level.getBlockState(leafPos))) {
                queue.add(new LeafSearchNode(leafPos.immutable(), nextDistance, false));
            }
        }
    }

    private static boolean isFacingAwayFromMain(final BlockPos leaf, final BlockPos foreignLog, final int toMainX, final int toMainZ) {
        final int fromForeignX = leaf.getX() - foreignLog.getX();
        final int fromForeignZ = leaf.getZ() - foreignLog.getZ();
        return fromForeignX * toMainX + fromForeignZ * toMainZ <= 0;
    }

    private static ClosestLogs findClosestLogs(final Set<BlockPos> mainLogs, final Set<BlockPos> foreignLogs) {
        ClosestLogs closest = null;
        for (final BlockPos mainLog : mainLogs) {
            for (final BlockPos foreignLog : foreignLogs) {
                final int distance = horizontalChebyshevDistance(mainLog, foreignLog);
                if (closest == null || distance < closest.distance) {
                    closest = new ClosestLogs(mainLog, foreignLog, distance);
                }
            }
        }
        return closest;
    }

    private static BlockPos nearestLog(final BlockPos pos, final Set<BlockPos> logs) {
        BlockPos nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (final BlockPos log : logs) {
            final int distance = horizontalChebyshevDistance(pos, log);
            if (distance < nearestDistance) {
                nearest = log;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int horizontalChebyshevDistance(final BlockPos first, final BlockPos second) {
        return Math.max(Math.abs(first.getX() - second.getX()), Math.abs(first.getZ() - second.getZ()));
    }

    private static void protectNearbyForeignTreeLeaves(final BlockGetter level, final Set<BlockPos> mainLogs, final Set<BlockPos> connectedToMinedBlock, final Set<BlockPos> protectedLeaves, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        final Set<BlockPos> checkedForeignLogs = new HashSet<>();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX - LEAF_PADDING; x <= maxX + LEAF_PADDING; x++) {
            for (int y = minY - LEAF_PADDING; y <= maxY + LEAF_PADDING; y++) {
                for (int z = minZ - LEAF_PADDING; z <= maxZ + LEAF_PADDING; z++) {
                    cursor.set(x, y, z);
                    final BlockPos candidate = cursor.immutable();
                    if (connectedToMinedBlock.contains(candidate) || checkedForeignLogs.contains(candidate) || !TreeUtil.isLog(level.getBlockState(candidate))) {
                        continue;
                    }

                    final Set<BlockPos> foreignLogs = findGroundedForeignLogs(level, candidate, connectedToMinedBlock);
                    checkedForeignLogs.add(candidate);
                    checkedForeignLogs.addAll(foreignLogs);
                    if (!foreignLogs.isEmpty()) {
                        protectLeavesNear(level, mainLogs, foreignLogs, protectedLeaves, minX, minY, minZ, maxX, maxY, maxZ);
                    }
                }
            }
        }
    }

    private static void collectLeaves(final BlockGetter level, final TreeResult result, final Set<BlockPos> protectedLeaves, final int cutY, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        final Set<BlockPos> visitedLeaves = new HashSet<>();
        final ArrayDeque<LeafSearchNode> queue = new ArrayDeque<>();

        for (final BlockPos logPos : result.logs()) {
            checkNeighborLeafBounds(level, protectedLeaves, cutY, logPos.getY() > cutY, minX, minY, minZ, maxX, maxY, maxZ, visitedLeaves, queue, logPos, 1);
        }

        while (!queue.isEmpty()) {
            if (result.leaves().size() > MAX_LEAVES) {
                return;
            }

            final LeafSearchNode current = queue.removeFirst();
            final BlockState currentState = level.getBlockState(current.pos);
            if (!isCollectableLeaf(currentState)) {
                continue;
            }

            result.addLeaf(current.pos, blockMass(level, current.pos));
            result.setLeafConnectionDistance(current.pos, current.distance);
            checkNeighborLeafBounds(level, protectedLeaves, cutY, current.wentAboveCutY, minX, minY, minZ, maxX, maxY, maxZ, visitedLeaves, queue, current.pos, current.distance + 1);
        }
    }

    private static void checkNeighborLeafBounds(final BlockGetter level, final Set<BlockPos> protectedLeaves, final int cutY, final boolean sourceWentAboveCutY, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ, final Set<BlockPos> visitedLeaves, final ArrayDeque<LeafSearchNode> queue, final BlockPos sourcePos, final int nextDistance) {
        if (nextDistance > MAX_LEAF_CONNECTION_DISTANCE) {
            return;
        }

        for (final Direction direction : CARDINAL_DIRECTIONS) {
            final BlockPos leafPos = sourcePos.relative(direction);
            final boolean wentAboveCutY = sourceWentAboveCutY || leafPos.getY() > cutY;
            if (leafPos.getY() < cutY && !wentAboveCutY) {
                continue;
            }

            if (isWithinLeafBounds(leafPos, minX, minY, minZ, maxX, maxY, maxZ)
                    && !protectedLeaves.contains(leafPos)
                    && visitedLeaves.add(leafPos.immutable())
                    && isCollectableLeaf(level.getBlockState(leafPos))) {
                final BlockPos immutableLeafPos = leafPos.immutable();
                queue.add(new LeafSearchNode(immutableLeafPos, nextDistance, wentAboveCutY));
            }
        }
    }

    private static void pruneDistantAttachedLeaves(final BlockGetter level, final TreeResult result, final Set<BlockPos> connectedLogs) {
        final Set<BlockPos> logsToRemove = result.logs();
        final Set<BlockPos> leavesToRemove = new HashSet<>(result.leaves());
        final Set<BlockPos> leavesToKeep = new HashSet<>();

        for (final BlockPos leaf : leavesToRemove) {
            final int range = nearestForeignMidpointRange(leaf, logsToRemove, connectedLogs);
            if (range >= 0 && result.leafConnectionDistance(leaf) > range && !wouldBeFloating(level, leaf, logsToRemove, leavesToRemove)) {
                leavesToKeep.add(leaf);
            }
        }

        for (final BlockPos leaf : leavesToKeep) {
            result.removeLeaf(leaf, blockMass(level, leaf));
        }
    }

    private static void collectAttachedBlocks(final BlockGetter level, final TreeResult result, final int cutY) {
        final Set<BlockPos> visited = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        for (final BlockPos log : result.logs()) {
            queue.add(log);
            visited.add(log);
        }
        for (final BlockPos leaf : result.leaves()) {
            queue.add(leaf);
            visited.add(leaf);
        }

        while (!queue.isEmpty()) {
            if (result.attachedBlocks().size() > MAX_ATTACHED_BLOCKS) {
                return;
            }

            final BlockPos current = queue.removeFirst();
            for (final Direction direction : CARDINAL_DIRECTIONS) {
                final BlockPos next = current.relative(direction);
                if (visited.contains(next) || result.logs().contains(next) || result.leaves().contains(next)) {
                    continue;
                }

                if (next.getY() >= cutY && TreeUtil.isAttachedBlock(level.getBlockState(next))) {
                    final BlockPos immutableNext = next.immutable();
                    visited.add(immutableNext);
                    result.addAttachedBlock(immutableNext, blockMass(level, immutableNext));
                    queue.add(immutableNext);
                }
            }
        }

        for (final BlockPos leaf : result.leaves()) {
            collectHangingAttachedBlocks(level, result, visited, leaf.below());
        }
    }

    private static void collectHangingAttachedBlocks(final BlockGetter level, final TreeResult result, final Set<BlockPos> visited, final BlockPos start) {
        BlockPos current = start;
        while (result.attachedBlocks().size() <= MAX_ATTACHED_BLOCKS && TreeUtil.isAttachedBlock(level.getBlockState(current))) {
            final BlockPos immutableCurrent = current.immutable();
            if (!visited.add(immutableCurrent)) {
                return;
            }

            result.addAttachedBlock(immutableCurrent, blockMass(level, immutableCurrent));
            current = current.below();
        }
    }

    private static double blockMass(final BlockGetter level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return PhysicsBlockPropertyHelper.getMass(level, pos, state);
    }

    private static int nearestForeignMidpointRange(final BlockPos leaf, final Set<BlockPos> mainLogs, final Set<BlockPos> connectedLogs) {
        final BlockPos closestMainLog = nearestLog(leaf, mainLogs);
        if (closestMainLog == null) {
            return -1;
        }

        int closestForeignDistance = Integer.MAX_VALUE;
        for (final BlockPos connectedLog : connectedLogs) {
            if (mainLogs.contains(connectedLog)) {
                continue;
            }
            final int distance = horizontalChebyshevDistance(closestMainLog, connectedLog);
            if (distance < closestForeignDistance) {
                closestForeignDistance = distance;
            }
        }

        return closestForeignDistance == Integer.MAX_VALUE ? -1 : closestForeignDistance / 2;
    }

    private static boolean wouldBeFloating(final BlockGetter level, final BlockPos leaf, final Set<BlockPos> logsToRemove, final Set<BlockPos> leavesToRemove) {
        for (final Direction direction : CARDINAL_DIRECTIONS) {
            final BlockPos neighbor = leaf.relative(direction);
            if (logsToRemove.contains(neighbor) || leavesToRemove.contains(neighbor)) {
                continue;
            }

            final BlockState neighborState = level.getBlockState(neighbor);
            if (!neighborState.isAir()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCollectableLeaf(final BlockState state) {
        return TreeUtil.isLeaf(state) && TreeUtil.isLeafIrresolute(state);
    }

    private static boolean isWithinLeafBounds(final BlockPos pos, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        return pos.getX() >= minX - LEAF_PADDING
                && pos.getX() <= maxX + LEAF_PADDING
                && pos.getY() >= minY - LEAF_PADDING
                && pos.getY() <= maxY + LEAF_PADDING
                && pos.getZ() >= minZ - LEAF_PADDING
                && pos.getZ() <= maxZ + LEAF_PADDING;
    }

    private static boolean isIgnored(final BlockPos pos, final BlockPos ignoredBrokenPos) {
        return ignoredBrokenPos != null && ignoredBrokenPos.equals(pos);
    }

    private static BlockPos[] buildNeighbors() {
        final BlockPos[] neighbors = new BlockPos[26];
        int index = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        neighbors[index++] = new BlockPos(x, y, z);
                    }
                }
            }
        }
        return neighbors;
    }

    private record ClosestLogs(BlockPos mainLog, BlockPos foreignLog, int distance) {
    }

    private record LeafSearchNode(BlockPos pos, int distance, boolean wentAboveCutY) {
    }

    private record LogComponent(Set<BlockPos> logs, boolean grounded, int highestY) {
        private boolean reachesCutHeight(final int cutY) {
            return highestY >= cutY;
        }
    }
}
