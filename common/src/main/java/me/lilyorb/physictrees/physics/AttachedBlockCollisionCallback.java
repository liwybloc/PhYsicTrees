package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import me.lilyorb.physictrees.tree.TreeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class AttachedBlockCollisionCallback implements BlockSubLevelCollisionCallback {
    public static final AttachedBlockCollisionCallback INSTANCE = new AttachedBlockCollisionCallback();

    private AttachedBlockCollisionCallback() {
    }

    @Override
    public CollisionResult sable$onCollision(final BlockPos pos, final @Nullable BlockPos otherHitBlockPos, final Vector3d hitPos, final double impactVelocity) {
        final SubLevelPhysicsSystem system = SubLevelPhysicsSystem.getCurrentlySteppingSystem();
        if (system == null) {
            return CollisionResult.NONE;
        }

        final ServerSubLevel subLevel = findAccessorySubLevel(system.getLevel(), pos, hitPos);
        if (subLevel != null && isBreakSurface(subLevel.getLevel(), otherHitBlockPos)) {
            TreePhysics.breakDetachedAccessories(subLevel);
        }

        return CollisionResult.NONE;
    }

    private static boolean isBreakSurface(final ServerLevel level, final @Nullable BlockPos otherHitBlockPos) {
        if (otherHitBlockPos == null || !level.isLoaded(otherHitBlockPos)) {
            return false;
        }

        final BlockState state = level.getBlockState(otherHitBlockPos);
        return !state.isAir()
                && !state.getCollisionShape(level, otherHitBlockPos).isEmpty()
                && !TreeUtil.isTreeBlock(state);
    }

    private static @Nullable ServerSubLevel findAccessorySubLevel(final ServerLevel level, final BlockPos pos, final Vector3d hitPos) {
        final SubLevel containing = Sable.HELPER.getContaining(level, pos);
        if (containing instanceof final ServerSubLevel containingSubLevel
                && !containingSubLevel.isRemoved()
                && TreePhysics.isDetachedAccessorySubLevel(containingSubLevel)) {
            return containingSubLevel;
        }

        final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(
                level,
                new BoundingBox3d(BlockPos.containing(hitPos.x, hitPos.y, hitPos.z))
        );

        for (final SubLevel candidate : intersecting) {
            if (candidate instanceof final ServerSubLevel serverSubLevel
                    && !serverSubLevel.isRemoved()
                    && TreePhysics.isDetachedAccessorySubLevel(serverSubLevel)) {
                return serverSubLevel;
            }
        }

        return null;
    }
}
