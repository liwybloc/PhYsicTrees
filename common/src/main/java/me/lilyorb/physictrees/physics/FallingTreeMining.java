package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import me.lilyorb.physictrees.tree.TreeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FallingTreeMining {
    private FallingTreeMining() {
    }

    public static float partialBreakProgress() {
        return (float) Math.clamp(TreePhysicsSettings.partialBreakProgress(), 0.0D, 0.95D);
    }

    public static float breakProgress(final Level level, final BlockPos pos, final BlockState state) {
        if (level instanceof final ServerLevel serverLevel && TreePhysics.isImpactedFallingTreeLog(serverLevel, pos)) {
            return (float) Math.clamp(TreePhysicsSettings.collisionBreakProgress(), 0.0D, 0.95D);
        }

        return clientBreakProgress(level, pos, state);
    }

    public static float clientBreakProgress(final Level level, final BlockPos pos, final BlockState state) {
        final SubLevel subLevel = getFallingTreeSubLevel(level, pos, state);
        if (subLevel == null) {
            return 0.0F;
        }

        return (float) Math.clamp(TreePhysicsSettings.collisionBreakProgress(), 0.0D, 0.95D);
    }

    public static boolean isFallingTreeLog(final Level level, final BlockPos pos, final BlockState state) {
        final boolean serverCache = level instanceof final ServerLevel serverLevel && TreePhysics.isImpactedFallingTreeLog(serverLevel, pos);
        final SubLevel subLevel = getFallingTreeSubLevel(level, pos, state);
        return serverCache || subLevel != null;
    }

    private static SubLevel getFallingTreeSubLevel(final Level level, final BlockPos pos, final BlockState state) {
        if (!TreeUtil.isLog(state)) {
            return null;
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        final boolean fallingTreeSubLevel = TreePhysics.isFallingTreeSubLevel(subLevel);
        return fallingTreeSubLevel ? subLevel : null;
    }
}
