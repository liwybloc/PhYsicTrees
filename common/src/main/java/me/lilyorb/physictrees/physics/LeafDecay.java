package me.lilyorb.physictrees.physics;

import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.core.TreePhysicsSettings;
import me.lilyorb.physictrees.tree.TreeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public final class LeafDecay {
    public static final Map<ServerLevel, Map<BlockPos, Integer>> leaves = new IdentityHashMap<>();

    public static void startDecay(final FallingTreeImpact impact) {
        if (!TreePhysicsSettings.leafDecay()) {
            return;
        }

        try(final ServerLevel level = impact.level()) {
            impact.leaves().forEach(leaf -> markLeaf(level, leaf));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void markLeaf(final ServerLevel level, final BlockPos leaf) {
        final int baseTicks = Math.max(1, (int) Math.round(TreePhysicsSettings.leafDecayTicks()));
        final int randomTicks = Math.max(0, (int) Math.round(TreePhysicsSettings.leafDecayTicksRandom()));
        final int delay = baseTicks + (randomTicks == 0 ? 0 : ThreadLocalRandom.current().nextInt(randomTicks + 1));
        leaves.computeIfAbsent(level, ignored -> new java.util.HashMap<>()).put(leaf.immutable(), delay);
    }

    public static void tick() {
        if (leaves.isEmpty()) {
            return;
        }

        final Iterator<Map.Entry<ServerLevel, Map<BlockPos, Integer>>> levelIterator = leaves.entrySet().iterator();
        while (levelIterator.hasNext()) {
            final Map.Entry<ServerLevel, Map<BlockPos, Integer>> levelEntry = levelIterator.next();
            tickLevel(levelEntry.getKey(), levelEntry.getValue());
            if (levelEntry.getValue().isEmpty()) {
                levelIterator.remove();
            }
        }
    }

    private static void tickLevel(final ServerLevel level, final Map<BlockPos, Integer> levelLeaves) {
        final Iterator<Map.Entry<BlockPos, Integer>> iterator = levelLeaves.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<BlockPos, Integer> entry = iterator.next();
            final BlockPos pos = entry.getKey();
            final int timer = entry.getValue();

            final BlockState state = level.getBlockState(pos);
            if (!TreeUtil.isLeaf(state)) {
                iterator.remove();
                continue;
            }

            if (timer <= 1) {
                decayLeaf(level, pos, state);
                iterator.remove();
            } else {
                entry.setValue(timer - 1);
            }
        }
    }

    private static void decayLeaf(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
            return;
        }

        Block.dropResources(state, level, pos);
        level.removeBlock(pos, false);
    }

}
