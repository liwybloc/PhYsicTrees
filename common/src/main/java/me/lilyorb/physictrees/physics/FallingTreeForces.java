package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.core.TreePhysicsSettings;
import me.lilyorb.physictrees.tree.TreeResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@UtilityClass
public final class FallingTreeForces {
    private static final List<PendingFallForce> PENDING_FALL_FORCES = new ArrayList<>();

    public static void queue(final ServerLevel level, final Player player, final BlockPos cutPos, final Set<BlockPos> logs, final TreeResult tree, final ServerSubLevel subLevel) {
        final Vec3 cutSide = chooseFallSide(player, cutPos, logs);
        final Vector3d fallDirection = new Vector3d(cutSide.x, 0.0D, cutSide.z).normalize();

        final Vector3d topImpulsePosition = findPushImpulsePoint(logs, cutPos, subLevel);
        final Vector3d baseImpulsePosition = toSubLevelPosition(cutPos, cutPos, subLevel);
        final double angularNudge = tree.totalMass() * TreePhysicsSettings.getAngularMultiplier();
        final int forceTicks = Math.max(1, TreePhysicsSettings.forceTicks());
        final double upwardNudge = (TreePhysicsSettings.baseUpwardNudge() * angularNudge) * (TreePhysicsSettings.breakCutBlock() ? 0.2D : 1.0D);
        final Vector3d topImpulse = new Vector3d(fallDirection).mul(angularNudge / forceTicks);
        topImpulse.y += upwardNudge / forceTicks;
        final Vector3d baseCounterImpulse = new Vector3d(fallDirection)
                .mul(-angularNudge *
                        (TreePhysicsSettings.breakCutBlock() ? TreePhysicsSettings.counterImpulseBreakRatio()
                                : TreePhysicsSettings.counterImpulseNoBreakRatio()) / forceTicks);

        final PendingFallForce force = new PendingFallForce(level, subLevel, topImpulsePosition, topImpulse, baseImpulsePosition, baseCounterImpulse, forceTicks);
        applyForceStep(force);
        force.remainingTicks--;
        if (force.remainingTicks > 0) {
            PENDING_FALL_FORCES.add(force);
        }
    }

    public static void tick() {
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

    private static void applyForceStep(final PendingFallForce force) {
        final RigidBodyHandle handle = SubLevelPhysicsSystem.get(force.level).getPhysicsHandle(force.subLevel);
        handle.applyImpulseAtPoint(new Vector3d(force.topImpulsePosition), new Vector3d(force.topImpulse));
        handle.applyImpulseAtPoint(new Vector3d(force.baseImpulsePosition), new Vector3d(force.baseCounterImpulse));
    }

    private static Vec3 chooseFallSide(final Player player, final BlockPos cutPos, final Set<BlockPos> logs) {
        final Vec3 rootBendSide = rootBendSide(cutPos, logs);
        if (rootBendSide != null) {
            return rootBendSide;
        }

        final Vec3 sideFromPlayerToCut = cutPos.getCenter().subtract(player.getEyePosition()).multiply(1.0D, 0.0D, 1.0D);
        final Vec3 fallbackSide = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        return snapToCardinal(chooseCutSide(sideFromPlayerToCut, fallbackSide));
    }

    private static Vec3 rootBendSide(final BlockPos cutPos, final Set<BlockPos> logs) {
        if (logs.contains(cutPos.above())) {
            return null;
        }

        for (final BlockPos log : logs) {
            if (!isDepthOneNeighbor(cutPos, log)) {
                continue;
            }

            final int dx = log.getX() - cutPos.getX();
            final int dz = log.getZ() - cutPos.getZ();
            if (dx != 0 || dz != 0) {
                return snapToCardinal(new Vec3(dx, 0.0D, dz));
            }
        }

        return null;
    }

    private static boolean isDepthOneNeighbor(final BlockPos root, final BlockPos candidate) {
        final int dx = Math.abs(candidate.getX() - root.getX());
        final int dy = Math.abs(candidate.getY() - root.getY());
        final int dz = Math.abs(candidate.getZ() - root.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1 && dx + dy + dz > 0;
    }

    private static Vector3d findPushImpulsePoint(final Set<BlockPos> logs, final BlockPos cutPos, final ServerSubLevel subLevel) {
        final Vector3d centerOfMass = centerOfMass(subLevel, cutPos);
        final double leverArm = pushLeverArm(logs, cutPos);
        return centerOfMass.add(0.0D, leverArm, 0.0D);
    }

    private static Vector3d centerOfMass(final ServerSubLevel subLevel, final BlockPos cutPos) {
        final MassData massData = subLevel.getMassTracker();
        if (massData != null && massData.getCenterOfMass() != null) {
            return new Vector3d(massData.getCenterOfMass());
        }

        return toSubLevelPosition(cutPos, cutPos, subLevel);
    }

    private static double pushLeverArm(final Set<BlockPos> logs, final BlockPos cutPos) {
        int highestY = cutPos.getY();
        int lowestY = cutPos.getY();
        for (final BlockPos pos : logs) {
            highestY = Math.max(highestY, pos.getY());
            lowestY = Math.min(lowestY, pos.getY());
        }

        final double treeHeight = Math.max(1.0D, highestY - lowestY + 1.0D);
        final double leverArm = treeHeight * TreePhysicsSettings.centerMassPushHeightRatio();
        return Math.clamp(
                leverArm,
                TreePhysicsSettings.minimumPushLeverArm(),
                TreePhysicsSettings.maximumPushLeverArm()
        );
    }

    private static Vector3d toSubLevelPosition(final BlockPos worldPos, final BlockPos cutPos, final ServerSubLevel subLevel) {
        final Vector3d worldOffsetFromCut = FallingTreeSubLevelData.blockCenter(worldPos)
                .sub(FallingTreeSubLevelData.blockCenter(cutPos), new Vector3d());
        return FallingTreeSubLevelData.blockCenter(subLevel.getPlot().getCenterBlock()).add(worldOffsetFromCut);
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

    private static Vec3 snapToCardinal(final Vec3 direction) {
        if (Math.abs(direction.x) >= Math.abs(direction.z)) {
            return new Vec3(Math.signum(direction.x), 0.0D, 0.0D);
        }
        return new Vec3(0.0D, 0.0D, Math.signum(direction.z));
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PendingFallForce {
        private final ServerLevel level;
        private final ServerSubLevel subLevel;
        private final Vector3d topImpulsePosition;
        private final Vector3d topImpulse;
        private final Vector3d baseImpulsePosition;
        private final Vector3d baseCounterImpulse;
        private int remainingTicks;
    }
}
