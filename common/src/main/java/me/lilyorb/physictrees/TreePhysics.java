package me.lilyorb.physictrees;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class TreePhysics {
    private static final int FORCE_TICKS = 8;
    private static final double TOTAL_ANGULAR_NUDGE = 1.1D;
    private static final List<PendingFallForce> PENDING_FALL_FORCES = new ArrayList<>();

    private TreePhysics() {
    }

    public static boolean spawnFallingTree(final ServerLevel level, final Player player, final BlockPos cutPos, final TreeResult tree) {
        final Set<BlockPos> blocks = new HashSet<>(tree.logs().size() + tree.leaves().size());
        blocks.addAll(tree.logs());
        blocks.addAll(tree.leaves());
        if (blocks.isEmpty()) {
            return false;
        }

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, cutPos, blocks, new BoundingBox3i(cutPos, cutPos));
        if (subLevel == null) {
            return false;
        }

        clearOriginalBlocks(level, blocks);
        queueFallForce(level, player, cutPos, blocks, subLevel);
        return true;
    }

    public static void tick(final MinecraftServer server) {
        final Iterator<PendingFallForce> iterator = PENDING_FALL_FORCES.iterator();
        while (iterator.hasNext()) {
            final PendingFallForce force = iterator.next();
            if (force.subLevel.isRemoved()) {
                iterator.remove();
                continue;
            }

            applyForceStep(force);
            force.remainingTicks--;
            if (force.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private static void clearOriginalBlocks(final ServerLevel level, final Set<BlockPos> blocks) {
        final BlockState barrier = Blocks.BARRIER.defaultBlockState();
        final BlockState air = Blocks.AIR.defaultBlockState();
        for (final BlockPos pos : blocks) {
            level.setBlock(pos, barrier, 2);
            level.setBlock(pos, air, 2);
        }
    }

    private static void queueFallForce(final ServerLevel level, final Player player, final BlockPos cutPos, final Set<BlockPos> blocks, final ServerSubLevel subLevel) {
        final Vec3 sideFromCutToPlayer = player.getEyePosition().subtract(cutPos.getCenter()).multiply(1.0D, 0.0D, 1.0D);
        final Vec3 fallbackSide = player.getLookAngle().multiply(-1.0D, 0.0D, -1.0D);
        final Vec3 cutSide = chooseCutSide(sideFromCutToPlayer, fallbackSide);
        final Vector3d towardPlayer = new Vector3d(cutSide.x, 0.0D, cutSide.z).normalize();
        final Vector3d fallDirection = new Vector3d(-towardPlayer.z, 0.0D, towardPlayer.x).normalize();
        final Vector3d angularVelocity = new Vector3d(0.0D, 1.0D, 0.0D).cross(fallDirection, new Vector3d()).mul(TOTAL_ANGULAR_NUDGE / FORCE_TICKS);
        final Vec3 center = findBlockCenter(blocks, cutPos);
        final Vec3 cutAnchor = cutPos.getCenter();
        final Vector3d anchorOffset = new Vector3d(cutAnchor.x - center.x, cutAnchor.y - center.y, cutAnchor.z - center.z);
        final Vector3d linearVelocity = angularVelocity.cross(anchorOffset, new Vector3d()).negate();
        final PendingFallForce force = new PendingFallForce(level, subLevel, linearVelocity, angularVelocity, FORCE_TICKS);
        applyForceStep(force);
        force.remainingTicks--;
        if (force.remainingTicks > 0) {
            PENDING_FALL_FORCES.add(force);
        }
    }

    private static void applyForceStep(final PendingFallForce force) {
        final RigidBodyHandle handle = SubLevelPhysicsSystem.get(force.level).getPhysicsHandle(force.subLevel);
        handle.addLinearAndAngularVelocity(new Vector3d(force.linearVelocity), new Vector3d(force.angularVelocity));
    }

    private static Vec3 findBlockCenter(final Set<BlockPos> blocks, final BlockPos cutPos) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        int count = 0;

        for (final BlockPos pos : blocks) {
            x += pos.getX() + 0.5D;
            y += pos.getY() + 0.5D;
            z += pos.getZ() + 0.5D;
            count++;
        }

        if (count == 0) {
            return cutPos.getCenter();
        }
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3 chooseCutSide(final Vec3 sideFromCutToPlayer, final Vec3 fallbackSide) {
        if (sideFromCutToPlayer.lengthSqr() > 1.0E-5D) {
            return sideFromCutToPlayer.normalize();
        }
        if (fallbackSide.lengthSqr() > 1.0E-5D) {
            return fallbackSide.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static final class PendingFallForce {
        private final ServerLevel level;
        private final ServerSubLevel subLevel;
        private final Vector3d linearVelocity;
        private final Vector3d angularVelocity;
        private int remainingTicks;

        private PendingFallForce(final ServerLevel level, final ServerSubLevel subLevel, final Vector3d linearVelocity, final Vector3d angularVelocity, final int remainingTicks) {
            this.level = level;
            this.subLevel = subLevel;
            this.linearVelocity = linearVelocity;
            this.angularVelocity = angularVelocity;
            this.remainingTicks = remainingTicks;
        }
    }
}
