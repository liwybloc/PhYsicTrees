package me.lilyorb.physictrees.tree;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@UtilityClass
public final class TreeUtil {
    public static boolean isLog(final BlockState state) {
        return state.is(TreeTags.LOGS) || state.is(BlockTags.LOGS);
    }

    public static boolean isLeaf(final BlockState state) {
        return state.is(TreeTags.LEAVES) || state.is(BlockTags.LEAVES);
    }

    public static boolean isTreeBlock(final BlockState state) {
        return state.is(TreeTags.TREE) || isLog(state) || isLeaf(state) || isAttachedBlock(state);
    }

    public static boolean isAttachedBlock(final BlockState state) {
        return state.is(TreeTags.ATTACHED_BLOCKS);
    }

    public static boolean isRoot(final BlockState state) {
        return state.is(TreeTags.ROOTS);
    }

    public static boolean canBeRoot(final BlockState state) {
        return state.is(TreeTags.CAN_BE_ROOTS) || state.is(BlockTags.DIRT);
    }

    public static boolean isSameLogType(final BlockState first, final BlockState second) {
        return first.getBlock() == second.getBlock();
    }

    public static Direction.Axis getLogAxis(final BlockState state) {
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            return state.getValue(RotatedPillarBlock.AXIS);
        }
        if (state.getBlock() instanceof HugeMushroomBlock) {
            if (!state.getValue(HugeMushroomBlock.UP) && !state.getValue(HugeMushroomBlock.DOWN)) {
                return Direction.Axis.Y;
            }
            if (!state.getValue(HugeMushroomBlock.EAST) && !state.getValue(HugeMushroomBlock.WEST)) {
                return Direction.Axis.X;
            }
            if (!state.getValue(HugeMushroomBlock.NORTH) && !state.getValue(HugeMushroomBlock.SOUTH)) {
                return Direction.Axis.Z;
            }
        }
        return Direction.Axis.Y;
    }

    public static boolean isLeafIrresolute(final BlockState state) {
        return !isLeaf(state) || !state.hasProperty(BlockStateProperties.PERSISTENT) || !state.getValue(BlockStateProperties.PERSISTENT);
    }

    public static int getLeafDistance(final BlockState state, final BlockPos leafPos, final BlockPos trunkStart) {
        if (state.hasProperty(LeavesBlock.DISTANCE)) {
            return state.getValue(LeavesBlock.DISTANCE);
        }
        final BlockPos sameHeightStart = new BlockPos(trunkStart.getX(), leafPos.getY(), trunkStart.getZ());
        return Math.clamp(leafPos.distManhattan(sameHeightStart), 1, BlockStateProperties.MAX_DISTANCE);
    }
}
