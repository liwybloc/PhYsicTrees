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
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@UtilityClass
public final class FallingTreeForces {
    private static final Vector3dc UP = new Vector3d(0.0D, 1.0D, 0.0D);

    private static final List<PendingFallForce> PENDING_FALL_FORCES = new ArrayList<>();

    public static void queue(final ServerLevel level, final Player player, final BlockPos cutPos, final Set<BlockPos> logs, final TreeResult tree, final ServerSubLevel subLevel) {
        final Vec3 cutSide = chooseFallSide(player, cutPos, logs);
        final Vector3d fallDirection = new Vector3d(cutSide.x, 0.0D, cutSide.z).normalize();

        final TreeForceScale scale = treeForceScale(logs, cutPos, tree);
        final int forceTicks = Math.max(1, TreePhysicsSettings.forceTicks());

        final PendingFallForce force = new PendingFallForce(level, subLevel, cutPos.immutable(), new Vector3d(fallDirection), scale, forceTicks, 0);
//        applyForceStep(force);
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

            if(force.bufferTicks > 0) {
                force.bufferTicks--;
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
        final MassData massData = force.subLevel.getMassTracker();
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return;
        }

        final Vector3d fallAxis = new Vector3d(UP).cross(force.fallDirection).normalize();
        final Vector3d currentAngularVelocity = handle.getAngularVelocity(new Vector3d());
        final double targetAngularVelocity = TreePhysicsSettings.targetFallAngularVelocity()
                * TreePhysicsSettings.getAngularMultiplier()
                * force.scale.angularVelocityScale();
        final double angularDelta = Math.max(0.0D, targetAngularVelocity - currentAngularVelocity.dot(fallAxis));
        final Vector3d angularImpulse = massData.getInertiaTensor().transform(new Vector3d(fallAxis).mul(angularDelta), new Vector3d());
        final Vector3d predictedAngularVelocity = new Vector3d(currentAngularVelocity).fma(angularDelta, fallAxis);

        final Vector3d currentLinearVelocity = handle.getLinearVelocity(new Vector3d());
        final double targetLinearVelocity = TreePhysicsSettings.targetFallLinearVelocity()
                * TreePhysicsSettings.centerMassImpulseRatio()
                * force.scale.translationScale();
        final double linearDelta = targetLinearVelocity - currentLinearVelocity.dot(force.fallDirection);
        final Vector3d linearImpulse = new Vector3d(force.fallDirection).mul(massData.getMass() * linearDelta);
        final Vector3d predictedLinearVelocity = new Vector3d(currentLinearVelocity).fma(linearDelta, force.fallDirection);

        handle.applyLinearAndAngularImpulse(linearImpulse, angularImpulse);

        final Vector3d centerOfMass = new Vector3d(massData.getCenterOfMass());
        final Vector3d basePoint = toSubLevelPosition(force.cutPos, force.cutPos, force.subLevel);
        final Vector3d baseOffset = basePoint.sub(centerOfMass, new Vector3d());
        final Vector3d predictedBaseVelocity = new Vector3d(predictedAngularVelocity).cross(baseOffset).add(predictedLinearVelocity);
        final double targetBaseForwardVelocity = TreePhysicsSettings.forwardFallVelocityNudge() * force.scale.forwardCorrectionScale();
        final double baseForwardDelta = Math.max(0.0D, targetBaseForwardVelocity - predictedBaseVelocity.dot(force.fallDirection));
        final double forceTicks = Math.max(1.0D, TreePhysicsSettings.forceTicks());
        final Vector3d correctiveImpulse = new Vector3d(force.fallDirection).mul(baseForwardDelta * massData.getMass());
        correctiveImpulse.y -= TreePhysicsSettings.downwardFallVelocityNudge() * massData.getMass() / forceTicks;
        handle.applyImpulseAtPoint(centerOfMass, correctiveImpulse);
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

    private static TreeForceScale treeForceScale(final Set<BlockPos> logs, final BlockPos cutPos, final TreeResult tree) {
        final int logCount = Math.max(1, logs.size());
        final int height = treeHeight(logs, cutPos);
        final double referenceLogs = Math.max(1.0D, TreePhysicsSettings.referenceForceLogCount());
        final double referenceHeight = Math.max(1.0D, TreePhysicsSettings.referenceForceHeight());
        final double angularVelocityScale = exponentScale(
                height / referenceHeight,
                TreePhysicsSettings.fallAngularVelocityHeightExponent()
        );
        final double translationScale = exponentScale(
                logCount / referenceLogs,
                TreePhysicsSettings.fallLinearVelocityLogExponent()
        );
        final double forwardCorrectionScale = exponentScale(
                logCount / referenceLogs,
                TreePhysicsSettings.forwardFallVelocityLogExponent()
        );
        return new TreeForceScale(angularVelocityScale, translationScale, forwardCorrectionScale);
    }

    private static int treeHeight(final Set<BlockPos> logs, final BlockPos cutPos) {
        int highestY = cutPos.getY();
        int lowestY = cutPos.getY();
        for (final BlockPos pos : logs) {
            highestY = Math.max(highestY, pos.getY());
            lowestY = Math.min(lowestY, pos.getY());
        }
        return Math.max(1, highestY - lowestY + 1);
    }

    private static double exponentScale(final double ratio, final double exponent) {
        return Math.pow(Math.max(1.0E-6D, ratio), exponent);
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
        private final BlockPos cutPos;
        private final Vector3d fallDirection;
        private final TreeForceScale scale;
        private int remainingTicks;
        private int bufferTicks;
    }

    private record TreeForceScale(double angularVelocityScale, double translationScale, double forwardCorrectionScale) {
    }
}
