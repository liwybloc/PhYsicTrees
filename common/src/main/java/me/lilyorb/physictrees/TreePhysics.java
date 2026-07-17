package me.lilyorb.physictrees;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.*;

public final class TreePhysics {
    private static final String FALLING_TREE_TAG = "physictrees_falling_tree";
    private static final int FORCE_TICKS = 2;
    private static final double BASE_ANGULAR_NUDGE = 2.0D;
    private static final double BASE_UPWARD_NUDGE = 2.1D;
    private static final double LOG_FORCE_AMPLITUDE = 0.7D;
    private static final double LEAF_FORCE_AMPLITUDE = 0.6D;
    private static final double COUNTER_IMPULSE_RATIO = 0.25D;
    private static final List<PendingFallForce> PENDING_FALL_FORCES = new ArrayList<>();

    private TreePhysics() {
    }

    public static boolean spawnFallingTree(final ServerLevel level, final Player player, final BlockPos cutPos, final TreeResult tree) {
        final Set<BlockPos> logs = new HashSet<>(tree.logs());
        if (TreePhysicsSettings.BREAK_CUT_BLOCK) {
            logs.remove(cutPos);
        }

        final Set<BlockPos> blocks = new HashSet<>(tree.logs().size() + tree.leaves().size());
        blocks.addAll(logs);
        blocks.addAll(tree.leaves());
        if (blocks.isEmpty()) {
            return false;
        }

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, cutPos, blocks, new BoundingBox3i(cutPos, cutPos));
        if (subLevel == null) {
            return false;
        }
        markFallingTreeSubLevel(subLevel);

        clearOriginalBlocks(level, blocks);
        if (TreePhysicsSettings.BREAK_CUT_BLOCK) {
            breakCutBlock(level, player, cutPos);
            System.out.println("break cut");
        }
        queueFallForce(level, player, cutPos, logs, tree, subLevel);
        return true;
    }

    public static boolean isFallingTreeSubLevel(final SubLevel subLevel) {
        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final CompoundTag tag = serverSubLevel.getUserDataTag();
            return tag != null && tag.getBoolean(FALLING_TREE_TAG);
        }
        return subLevel != null;
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

    private static void breakCutBlock(final ServerLevel level, final Player player, final BlockPos cutPos) {
        final BlockState state = level.getBlockState(cutPos);
        Block.dropResources(state, level, cutPos, level.getBlockEntity(cutPos), player, player.getMainHandItem());
        level.setBlock(cutPos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void markFallingTreeSubLevel(final ServerSubLevel subLevel) {
        final CompoundTag tag = subLevel.getUserDataTag() == null ? new CompoundTag() : subLevel.getUserDataTag();
        tag.putBoolean(FALLING_TREE_TAG, true);
        subLevel.setUserDataTag(tag);
    }

    private static void queueFallForce(final ServerLevel level, final Player player, final BlockPos cutPos, final Set<BlockPos> logs, final TreeResult tree, final ServerSubLevel subLevel) {
        final Vec3 sideFromPlayerToCut = cutPos.getCenter().subtract(player.getEyePosition()).multiply(1.0D, 0.0D, 1.0D);
        final Vec3 fallbackSide = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        final Vec3 cutSide = snapToCardinal(chooseCutSide(sideFromPlayerToCut, fallbackSide));
        final Vector3d fallDirection = new Vector3d(cutSide.x, 0.0D, cutSide.z).normalize();

        final Vector3d topImpulsePosition = findTopImpulsePoint(logs, cutPos, subLevel);
        final Vector3d baseImpulsePosition = toSubLevelPosition(cutPos, cutPos, subLevel);
        final double forceScale = getForceScale(tree);
        final double angularNudge = BASE_ANGULAR_NUDGE + forceScale;
        final double upwardNudge = TreePhysicsSettings.BREAK_CUT_BLOCK ? 0.0D : BASE_UPWARD_NUDGE * forceScale;
        final Vector3d topImpulse = new Vector3d(fallDirection).mul(angularNudge / FORCE_TICKS);
        topImpulse.y += upwardNudge / FORCE_TICKS;
        final Vector3d baseCounterImpulse = new Vector3d(fallDirection)
                .mul(-angularNudge * (TreePhysicsSettings.BREAK_CUT_BLOCK ? COUNTER_IMPULSE_RATIO * -0.1 : COUNTER_IMPULSE_RATIO)
                        / FORCE_TICKS);

        final PendingFallForce force = new PendingFallForce(level, subLevel, topImpulsePosition, topImpulse, baseImpulsePosition, baseCounterImpulse, FORCE_TICKS);
        applyForceStep(force);
        force.remainingTicks--;
        if (force.remainingTicks > 0) {
            PENDING_FALL_FORCES.add(force);
        }
    }

    private static double getForceScale(final TreeResult tree) {
        return tree.logs().size() * LOG_FORCE_AMPLITUDE + tree.leaves().size() * LEAF_FORCE_AMPLITUDE;
    }

    private static Vector3d blockCenter(final BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static void applyForceStep(final PendingFallForce force) {
        final RigidBodyHandle handle = SubLevelPhysicsSystem.get(force.level).getPhysicsHandle(force.subLevel);
        handle.applyImpulseAtPoint(new Vector3d(force.topImpulsePosition), new Vector3d(force.topImpulse));
        handle.applyImpulseAtPoint(new Vector3d(force.baseImpulsePosition), new Vector3d(force.baseCounterImpulse));
    }

    private static Vector3d findTopImpulsePoint(final Set<BlockPos> logs, final BlockPos cutPos, final ServerSubLevel subLevel) {
        BlockPos highest = cutPos;
        for (final BlockPos pos : logs) {
            if (pos.getY() > highest.getY()) {
                highest = pos;
            }
        }

        return toSubLevelPosition(highest, cutPos, subLevel);
    }

    private static Vector3d toSubLevelPosition(final BlockPos worldPos, final BlockPos cutPos, final ServerSubLevel subLevel) {
        final Vector3d worldOffsetFromCut = blockCenter(worldPos).sub(blockCenter(cutPos), new Vector3d());
        return blockCenter(subLevel.getPlot().getCenterBlock()).add(worldOffsetFromCut);
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

    private static final class PendingFallForce {
        private final ServerLevel level;
        private final ServerSubLevel subLevel;
        private final Vector3d topImpulsePosition;
        private final Vector3d topImpulse;
        private final Vector3d baseImpulsePosition;
        private final Vector3d baseCounterImpulse;
        private int remainingTicks;

        private PendingFallForce(final ServerLevel level, final ServerSubLevel subLevel, final Vector3d topImpulsePosition, final Vector3d topImpulse, final Vector3d baseImpulsePosition, final Vector3d baseCounterImpulse, final int remainingTicks) {
            this.level = level;
            this.subLevel = subLevel;
            this.topImpulsePosition = topImpulsePosition;
            this.topImpulse = topImpulse;
            this.baseImpulsePosition = baseImpulsePosition;
            this.baseCounterImpulse = baseCounterImpulse;
            this.remainingTicks = remainingTicks;
        }
    }
}
