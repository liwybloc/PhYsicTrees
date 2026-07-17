package me.lilyorb.physictrees;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FallingTreeMining {
    private FallingTreeMining() {
    }

    public static float partialBreakProgress() {
        return (float) Math.clamp(TreePhysicsSettings.PARTIAL_BREAK_PROGRESS, 0.0D, 0.95D);
    }

    public static boolean isFallingTreeLog(final Level level, final BlockPos pos, final BlockState state) {
        if (!TreeUtil.isLog(state)) {
            return false;
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        return TreePhysics.isFallingTreeSubLevel(subLevel);
    }
}
