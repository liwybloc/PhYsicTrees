package me.lilyorb.physictrees;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class TreeFelling {
    private TreeFelling() {
    }

    public static boolean hasValidHeldItem(final Player player) {
//        final ItemStack heldItem = player.getMainHandItem();
//        return heldItem.isEmpty() || heldItem.is(ItemTags.AXES);
        return true; // ignore frfr
    }

    public static boolean canFell(final Player player) {
        return player instanceof ServerPlayer serverPlayer
                && serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                && !player.isShiftKeyDown()
                && hasValidHeldItem(player);
    }

    public static boolean isValidTreeTarget(final Level level, final BlockPos pos) {
        return findTreeTarget(level, pos) != null;
    }

    public static TreeResult findTreeTarget(final Level level, final BlockPos pos) {
        if (!TreeUtil.isLog(level.getBlockState(pos))) {
            return null;
        }

        if (!TreePhysicsSettings.ALLOW_LOWER_BLOCK_FELLING && isLowerBlockTarget(level, pos)) {
            return null;
        }

        return TreeFloodFill.findTree(level, pos, null);
    }

    public static boolean tryFell(final Level level, final Player player, final BlockPos pos, final BlockState minedState) {
        if (level.isClientSide() || !canFell(player) || !TreeUtil.isLog(minedState)) {
            return false;
        }

        final TreeResult tree = findTreeTarget(level, pos);
        if (tree == null) {
            return false;
        }

        if (level instanceof ServerLevel serverLevel && TreePhysics.spawnFallingTree(serverLevel, player, pos, tree)) {
            return true;
        }

        final BlockState air = Blocks.AIR.defaultBlockState();
        for (final BlockPos leafPos : tree.leaves()) {
            level.setBlock(leafPos, air, 3);
        }
        for (final BlockPos logPos : tree.logs()) {
            if (TreePhysicsSettings.BREAK_CUT_BLOCK && logPos.equals(pos)) {
                Block.dropResources(minedState, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
                level.setBlock(logPos, air, 3);
                continue;
            }
            level.setBlock(logPos, air, 3);
        }
        return true;
    }

    private static boolean isLowerBlockTarget(final Level level, final BlockPos pos) {
        final BlockState below = level.getBlockState(pos.below());
        return TreeUtil.isRoot(below) || TreeUtil.canBeRoot(below);
    }

}
