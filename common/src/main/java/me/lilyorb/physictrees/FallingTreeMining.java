package me.lilyorb.physictrees;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FallingTreeMining {
    private FallingTreeMining() {
    }

    public static float partialBreakProgress() {
        return (float) Math.clamp(TreePhysicsSettings.PARTIAL_BREAK_PROGRESS, 0.0D, 0.95D);
    }

    public static float breakProgress(final Level level, final BlockPos pos, final BlockState state) {
        if (level instanceof final ServerLevel serverLevel && TreePhysics.isImpactedFallingTreeLog(serverLevel, pos)) {
            return (float) Math.clamp(TreePhysicsSettings.COLLISION_BREAK_PROGRESS, 0.0D, 0.95D);
        }

        final SubLevel subLevel = getFallingTreeSubLevel(level, pos, state);
        if (subLevel == null) {
            return 0.0F;
        }

        final double progress = TreePhysics.isImpactedFallingTreeSubLevel(subLevel)
                ? TreePhysicsSettings.COLLISION_BREAK_PROGRESS
                : TreePhysicsSettings.PARTIAL_BREAK_PROGRESS;
        return (float) Math.clamp(progress, 0.0D, 0.95D);
    }

    public static float clientBreakProgress(final Level level, final BlockPos pos, final BlockState state) {
        final SubLevel subLevel = getFallingTreeSubLevel(level, pos, state);
        if (subLevel == null) {
            return 0.0F;
        }

        return (float) Math.clamp(TreePhysicsSettings.COLLISION_BREAK_PROGRESS, 0.0D, 0.95D);
    }

    public static boolean isFallingTreeLog(final Level level, final BlockPos pos, final BlockState state) {
        return level instanceof final ServerLevel serverLevel && TreePhysics.isImpactedFallingTreeLog(serverLevel, pos)
                || getFallingTreeSubLevel(level, pos, state) != null;
    }

    private static SubLevel getFallingTreeSubLevel(final Level level, final BlockPos pos, final BlockState state) {
        if (!TreeUtil.isLog(state)) {
            return null;
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        return TreePhysics.isFallingTreeSubLevel(subLevel) ? subLevel : null;
    }
}
