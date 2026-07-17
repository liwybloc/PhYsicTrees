package me.lilyorb.physictrees;

import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public final class LeafCollisionCallback extends FragileBlockCallback {
    public static final LeafCollisionCallback INSTANCE = new LeafCollisionCallback();

    private LeafCollisionCallback() {
    }

    @Override
    public double getTriggerVelocity() {
        return 0.0D;
    }

    @Override
    public boolean shouldTriggerFor(final BlockState state) {
        return TreeUtil.isLeaf(state) && !TreeUtil.isLeafPersistent(state);
    }

    @Override
    public CollisionResult sable$onCollision(final BlockPos pos, final BlockPos otherHitBlockPos, final Vector3d hitPos, final double impactVelocity) {
        return super.sable$onCollision(pos, otherHitBlockPos, hitPos, impactVelocity);
    }
}
