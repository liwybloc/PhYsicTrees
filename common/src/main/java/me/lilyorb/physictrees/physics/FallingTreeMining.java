package me.lilyorb.physictrees.physics;

import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.core.TreePhysicsSettings;
import me.lilyorb.physictrees.tree.TreeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public final class FallingTreeMining {
    private static final Set<BlockPos> CLIENT_COLLISION_DAMAGED_LOGS = new HashSet<>();

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
        if (!TreeUtil.isLog(state) || !CLIENT_COLLISION_DAMAGED_LOGS.contains(pos)) {
            return 0.0F;
        }

        return (float) Math.clamp(TreePhysicsSettings.collisionBreakProgress(), 0.0D, 0.95D);
    }

    public static boolean isFallingTreeLog(final Level level, final BlockPos pos, final BlockState state) {
        return TreeUtil.isLog(state) && level instanceof final ServerLevel serverLevel && TreePhysics.isImpactedFallingTreeLog(serverLevel, pos);
    }

    public static void markClientCollisionDamaged(final BlockPos pos, final boolean damaged) {
        final BlockPos immutablePos = pos.immutable();
        if (damaged) {
            CLIENT_COLLISION_DAMAGED_LOGS.add(immutablePos);
        } else {
            CLIENT_COLLISION_DAMAGED_LOGS.remove(immutablePos);
        }
    }

    public static void clearClientCollisionDamage() {
        CLIENT_COLLISION_DAMAGED_LOGS.clear();
    }
}
