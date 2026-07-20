package me.lilyorb.physictrees.particle;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.core.TreePhysicsSettings;
import me.lilyorb.physictrees.physics.CollisionDamage;
import me.lilyorb.physictrees.physics.FallingTreeImpact;
import me.lilyorb.physictrees.physics.TreePhysics;
import me.lilyorb.physictrees.tree.TreeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public final class CollisionParticles {
    private static final List<PendingBlockImpactParticle> PENDING_BLOCK_IMPACT_PARTICLES = new ArrayList<>();
    private static final Map<ServerSubLevel, Set<BlockPos>> IMPACT_PARTICLE_LOGS = new HashMap<>();

    public static void queueBlockImpact(final ServerLevel level, final ServerSubLevel subLevel, final BlockPos logPos, final BlockState collidedState, final BlockPos collidedPos) {
        if (!TreePhysics.isFallingTreeSubLevel(subLevel) || subLevel.isRemoved()) {
            return;
        }

        final Set<BlockPos> impactedLogs = IMPACT_PARTICLE_LOGS.computeIfAbsent(subLevel, key -> new HashSet<>());
        final BlockPos immutableLogPos = logPos.immutable();
        if (impactedLogs.add(immutableLogPos)) {
            final Vector3d worldLogCenter = Sable.HELPER.projectOutOfSubLevel(level, blockCenter(logPos), new Vector3d());
            PENDING_BLOCK_IMPACT_PARTICLES.add(new PendingBlockImpactParticle(level, worldLogCenter, new CollisionDustParticleOptions(collidedState, collidedPos.immutable())));
        }
    }

    public static void queueContactingLogImpacts() {
        for (final FallingTreeImpact impact : new ArrayList<>(CollisionDamage.impactedTrees())) {
            final ServerSubLevel subLevel = impact.subLevel();
            if (subLevel.isRemoved()) {
                continue;
            }

            final ServerLevel level = subLevel.getLevel();
            impact.logs().forEach(plotPos -> {
                final Set<BlockPos> impactedLogs = IMPACT_PARTICLE_LOGS.get(subLevel);
                if (impactedLogs != null && impactedLogs.contains(plotPos)) {
                    return;
                }

                final Vector3d worldLogCenter = Sable.HELPER.projectOutOfSubLevel(level, blockCenter(plotPos), new Vector3d());
                final BlockContact contact = findBlockContact(level, worldLogCenter);
                if (contact != null) {
                    queueBlockImpact(level, subLevel, plotPos, contact.state(), contact.pos());
                }
            });
        }
    }

    public static void flush() {
        final Iterator<PendingBlockImpactParticle> iterator = PENDING_BLOCK_IMPACT_PARTICLES.iterator();
        while (iterator.hasNext()) {
            final PendingBlockImpactParticle particle = iterator.next();
            iterator.remove();
            sendWorldParticlePacket(particle);
        }
    }

    private static void sendWorldParticlePacket(final PendingBlockImpactParticle particle) {
        final double x = particle.pos.x;
        final double y = particle.pos.y + TreePhysicsSettings.collisionParticleYOffset();
        final double z = particle.pos.z;
        particle.level.sendParticles(
                particle.options,
                x,
                y,
                z,
                Math.max(0, TreePhysicsSettings.collisionParticleCount()),
                TreePhysicsSettings.collisionParticleOffsetX(),
                TreePhysicsSettings.collisionParticleOffsetY(),
                TreePhysicsSettings.collisionParticleOffsetZ(),
                TreePhysicsSettings.collisionParticleSpeed()
        );
    }

    public static void removeTrackedLogs(final ServerSubLevel subLevel) {
        IMPACT_PARTICLE_LOGS.remove(subLevel);
    }

    private static BlockContact findBlockContact(final ServerLevel level, final Vector3d worldLogCenter) {
        final double probeDistance = TreePhysicsSettings.impactProbeDistance();
        final double[][] probes = {
                {0.0D, -probeDistance, 0.0D},
                {probeDistance, 0.0D, 0.0D},
                {-probeDistance, 0.0D, 0.0D},
                {0.0D, 0.0D, probeDistance},
                {0.0D, 0.0D, -probeDistance},
                {0.0D, probeDistance, 0.0D}
        };

        for (final double[] probe : probes) {
            final BlockPos pos = BlockPos.containing(worldLogCenter.x + probe[0], worldLogCenter.y + probe[1], worldLogCenter.z + probe[2]);
            if (!level.isLoaded(pos)) {
                continue;
            }

            final BlockState state = level.getBlockState(pos);
            if (isValidImpactSurface(state, level, pos)) {
                return new BlockContact(state, pos.immutable());
            }
        }

        return null;
    }

    private static boolean isValidImpactSurface(final BlockState state, final Level level, final BlockPos pos) {
        return !state.isAir() && !TreeUtil.isLeaf(state) && !TreeUtil.isLog(state) && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static Vector3d blockCenter(final BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private record PendingBlockImpactParticle(ServerLevel level, Vector3d pos, CollisionDustParticleOptions options) {
    }

    private record BlockContact(BlockState state, BlockPos pos) {
    }
}
