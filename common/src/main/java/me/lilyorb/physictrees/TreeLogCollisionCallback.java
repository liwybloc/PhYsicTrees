package me.lilyorb.physictrees;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class TreeLogCollisionCallback implements BlockSubLevelCollisionCallback {
    public static final TreeLogCollisionCallback INSTANCE = new TreeLogCollisionCallback();

    private TreeLogCollisionCallback() {
    }

    @Override
    public CollisionResult sable$onCollision(final BlockPos pos, final @Nullable BlockPos otherHitBlockPos, final Vector3d hitPos, final double impactVelocity) {
        final SubLevelPhysicsSystem system = SubLevelPhysicsSystem.getCurrentlySteppingSystem();
        if (system == null) {
            return CollisionResult.NONE;
        }

        final ServerSubLevel subLevel = findImpactedSubLevel(system.getLevel(), pos, hitPos);
        if (subLevel != null) {
            TreePhysics.markFallingTreeImpacted(subLevel);
            if (isGroundImpact(system.getLevel(), otherHitBlockPos)) {
                TreePhysics.queueGroundImpactParticles(system.getLevel(), subLevel, pos, hitPos);
            }
        }

        return CollisionResult.NONE;
    }

    private static boolean isGroundImpact(final ServerLevel level, final @Nullable BlockPos otherHitBlockPos) {
        if (otherHitBlockPos == null || !level.hasChunkAt(otherHitBlockPos)) {
            return false;
        }

        final BlockState state = level.getBlockState(otherHitBlockPos);
        return state.is(BlockTags.DIRT);
    }

    private static @Nullable ServerSubLevel findImpactedSubLevel(final ServerLevel level, final BlockPos pos, final Vector3d hitPos) {
        final SubLevel containing = Sable.HELPER.getContaining(level, pos);
        if (containing instanceof final ServerSubLevel containingSubLevel
                && !containingSubLevel.isRemoved()
                && TreePhysics.isFallingTreeSubLevel(containingSubLevel)) {
            return containingSubLevel;
        }

        final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(
                level,
                new BoundingBox3d(BlockPos.containing(hitPos.x, hitPos.y, hitPos.z))
        );

        for (final SubLevel candidate : intersecting) {
            if (candidate instanceof final ServerSubLevel serverSubLevel
                    && !serverSubLevel.isRemoved()
                    && TreePhysics.isFallingTreeSubLevel(serverSubLevel)) {
                return serverSubLevel;
            }
        }

        return null;
    }
}
