package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.core.TreePhysicsSettings;
import me.lilyorb.physictrees.particle.CollisionParticles;
import me.lilyorb.physictrees.tree.TreeResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public final class TreePhysics {
    public static boolean spawnFallingTree(final ServerLevel level, final Player player, final BlockPos cutPos, final TreeResult tree) {
        final Set<BlockPos> logs = new HashSet<>(tree.logs());
        if (TreePhysicsSettings.breakCutBlock()) {
            logs.remove(cutPos);
        }

        final Set<BlockPos> blocks = new HashSet<>(tree.logs().size() + tree.leaves().size() + tree.attachedBlocks().size());
        blocks.addAll(logs);
        blocks.addAll(tree.leaves());
        blocks.addAll(tree.attachedBlocks());
        if (blocks.isEmpty()) {
            return false;
        }

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, cutPos, blocks, new BoundingBox3i(cutPos, cutPos));
        if (subLevel == null) {
            return false;
        }

        FallingTreeSubLevelData.markFallingTree(subLevel, logs, tree.leaves(), tree.attachedBlocks(), cutPos, player);
        clearOriginalBlocks(level, blocks);
        if (TreePhysicsSettings.breakCutBlock()) {
            breakCutBlock(level, player, cutPos);
        }
        FallingTreeForces.queue(level, player, cutPos, logs, tree, subLevel);
        TreePhysicsSounds.playCreak(level, subLevel);
        return true;
    }

    public static boolean isFallingTreeSubLevel(final SubLevel subLevel) {
        return FallingTreeSubLevelData.isFallingTree(subLevel);
    }

    public static boolean isImpactedFallingTreeSubLevel(final SubLevel subLevel) {
        return FallingTreeSubLevelData.isImpactedFallingTree(subLevel);
    }

    public static boolean isDetachedAccessorySubLevel(final SubLevel subLevel) {
        return AttachedBlockPhysics.isDetachedAccessorySubLevel(subLevel);
    }

    public static boolean isImpactedFallingTreeLog(final ServerLevel level, final BlockPos pos) {
        return CollisionDamage.isImpactedFallingTreeLog(level, pos);
    }

    public static boolean isCollisionDamageProgressPacket(final int id, final BlockPos pos) {
        return CollisionDamage.isCollisionDamageProgressPacket(id, pos);
    }

    public static void suppressCollisionDamageWhileMining(final ServerLevel level, final BlockPos pos) {
        CollisionDamage.suppressWhileMining(level, pos);
    }

    public static void stopSuppressingCollisionDamageWhileMining(final ServerLevel level, final BlockPos pos) {
        CollisionDamage.stopSuppressingWhileMining(level, pos);
    }

    public static void markFallingTreeImpacted(final ServerSubLevel subLevel) {
        if (!isFallingTreeSubLevel(subLevel) || isImpactedFallingTreeSubLevel(subLevel)) {
            return;
        }

        FallingTreeSubLevelData.markImpacted(subLevel);
        final FallingTreeImpact impact = FallingTreeSubLevelData.impactSnapshot(subLevel);
        CollisionDamage.trackImpacted(impact);
        AttachedBlockPhysics.queueDetach(impact);
        FloatingTreeDecay.makeFallingTreeDecayable(impact);
        LeafDecay.startDecay(impact);
    }

    public static void queueBlockImpactParticles(final ServerLevel level, final ServerSubLevel subLevel, final BlockPos logPos, final Vector3d hitPos, final BlockState collidedState, final BlockPos collidedPos) {
        CollisionParticles.queueBlockImpact(level, subLevel, logPos, collidedState, collidedPos);
    }

    public static void breakDetachedAccessories(final ServerSubLevel subLevel) {
        AttachedBlockPhysics.breakDetachedAccessories(subLevel);
    }

    public static void tick(final MinecraftServer server) {
        AttachedBlockPhysics.tick();
        FallingTreeForces.tick();
        CollisionDamage.tickMiningSuppression();
        CollisionDamage.refresh();
        CollisionParticles.queueContactingLogImpacts();
        CollisionParticles.flush();
        LeafDecay.tick();
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
}
