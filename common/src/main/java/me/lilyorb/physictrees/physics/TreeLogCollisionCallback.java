package me.lilyorb.physictrees.physics;

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
import org.joml.Vector3dc;

public final class TreeLogCollisionCallback implements BlockSubLevelCollisionCallback {
    public static final TreeLogCollisionCallback INSTANCE = new TreeLogCollisionCallback();
    private static final Vector3dc UP = new Vector3d(0.0D, 1.0D, 0.0D);
    private static final double IMPACT_UPRIGHTNESS_THRESHOLD = 0.75D;

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
            final ServerLevel level = subLevel.getLevel();
            final BlockState collidedState = getCollidedState(level, otherHitBlockPos);
            if (collidedState != null) {
                TreePhysics.queueBlockImpactParticles(level, subLevel, pos, hitPos, collidedState, otherHitBlockPos);
                if (uprightness(subLevel) < IMPACT_UPRIGHTNESS_THRESHOLD) {
                    TreePhysicsSounds.playImpact(level, subLevel);
                    TreePhysics.markFallingTreeImpacted(subLevel);
                } else {
                    TreePhysicsSounds.playCreak(level, subLevel);
                }
            }
        }

        return CollisionResult.NONE;
    }

    private static @Nullable BlockState getCollidedState(final ServerLevel level, final @Nullable BlockPos otherHitBlockPos) {
        if (otherHitBlockPos == null || !level.isLoaded(otherHitBlockPos)) {
            return null;
        }

        final BlockState state = level.getBlockState(otherHitBlockPos);
        return state.isAir() || state.getCollisionShape(level, otherHitBlockPos).isEmpty() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) ? null : state;
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

    private static double uprightness(final ServerSubLevel subLevel) {
        final Vector3d direction = subLevel.logicalPose().transformNormal(UP, new Vector3d());
        return Math.max(0.0D, direction.dot(UP));
    }
}
