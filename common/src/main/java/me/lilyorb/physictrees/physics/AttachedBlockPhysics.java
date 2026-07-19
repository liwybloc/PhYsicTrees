package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@UtilityClass
public final class AttachedBlockPhysics {
    private static final String DETACHED_ACCESSORY_TAG = "physictrees_detached_accessory";
    private static final String BROKEN_ACCESSORY_TAG = "physictrees_broken_accessory";
    private static final Set<FallingTreeImpact> PENDING_ACCESSORY_DETACHES = new HashSet<>();

    public static boolean isDetachedAccessorySubLevel(final SubLevel subLevel) {
        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final CompoundTag tag = serverSubLevel.getUserDataTag();
            return tag != null && tag.getBoolean(DETACHED_ACCESSORY_TAG) && !tag.getBoolean(BROKEN_ACCESSORY_TAG);
        }
        return false;
    }

    public static void queueDetach(final FallingTreeImpact impact) {
        PENDING_ACCESSORY_DETACHES.add(impact);
    }

    public static void tick() {
        final Iterator<FallingTreeImpact> iterator = PENDING_ACCESSORY_DETACHES.iterator();
        while (iterator.hasNext()) {
            final FallingTreeImpact impact = iterator.next();
            iterator.remove();
            final ServerSubLevel subLevel = impact.subLevel();
            if (!subLevel.isRemoved() && TreePhysics.isImpactedFallingTreeSubLevel(subLevel)) {
                detachAttachedBlocksOnImpact(impact);
            }
        }
    }

    public static void breakDetachedAccessories(final ServerSubLevel subLevel) {
        if (!isDetachedAccessorySubLevel(subLevel)) {
            return;
        }

        final CompoundTag tag = FallingTreeSubLevelData.mutableTag(subLevel);
        tag.putBoolean(BROKEN_ACCESSORY_TAG, true);
        subLevel.setUserDataTag(tag);

        final ServerLevel level = subLevel.getLevel();
        final Player player = FallingTreeSubLevelData.getCutter(level, subLevel);
        FallingTreeSubLevelData.forEachStoredAttachedBlockPosition(subLevel, plotPos -> breakAttachedBlock(level, plotPos, player));
    }

    private static void detachAttachedBlocksOnImpact(final FallingTreeImpact impact) {
        final List<Set<BlockPos>> components = attachedBlockComponents(impact.attachedBlocks());
        if (components.isEmpty()) {
            return;
        }

        final ServerSubLevel subLevel = impact.subLevel();
        final ServerLevel level = subLevel.getLevel();
        final Player cutter = FallingTreeSubLevelData.getCutter(level, subLevel);
        for (final Set<BlockPos> component : components) {
            final BlockPos anchor = component.iterator().next();
            final ServerSubLevel accessorySubLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, component, FallingTreeSubLevelData.boundsFor(component));
            if (accessorySubLevel != null) {
                markDetachedAccessorySubLevel(accessorySubLevel, component, anchor, cutter);
            }
        }
    }

    private static void breakAttachedBlock(final ServerLevel level, final BlockPos plotPos, final Player player) {
        final BlockState state = level.getBlockState(plotPos);
        if (state.isAir()) {
            return;
        }

        final Vector3d projectedCenter = Sable.HELPER.projectOutOfSubLevel(level, FallingTreeSubLevelData.blockCenter(plotPos), new Vector3d());
        final BlockPos breakPos = BlockPos.containing(projectedCenter.x, projectedCenter.y, projectedCenter.z);
        final BlockEntity blockEntity = copyBlockEntityToBreakPos(level, plotPos, breakPos, state);
        if (player == null) {
            Block.dropResources(state, level, breakPos, blockEntity);
            level.setBlock(plotPos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        state.getBlock().playerDestroy(level, player, breakPos, state, blockEntity, player.getMainHandItem());
        level.setBlock(plotPos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static BlockEntity copyBlockEntityToBreakPos(final ServerLevel level, final BlockPos sourcePos, final BlockPos breakPos, final BlockState state) {
        final BlockEntity source = level.getBlockEntity(sourcePos);
        if (source == null) {
            return null;
        }

        final CompoundTag tag = source.saveWithFullMetadata(level.registryAccess());
        tag.putInt("x", breakPos.getX());
        tag.putInt("y", breakPos.getY());
        tag.putInt("z", breakPos.getZ());
        final BlockEntity copy = BlockEntity.loadStatic(breakPos, state, tag, level.registryAccess());
        if (copy != null) {
            copy.setLevel(level);
        }
        return copy;
    }

    private static void markDetachedAccessorySubLevel(final ServerSubLevel subLevel, final Set<BlockPos> sourceComponent, final BlockPos anchor, final Player cutter) {
        final CompoundTag tag = FallingTreeSubLevelData.mutableTag(subLevel);
        tag.putBoolean(DETACHED_ACCESSORY_TAG, true);
        tag.put(FallingTreeSubLevelData.FALLING_TREE_ATTACHED_BLOCKS_TAG, FallingTreeSubLevelData.serializeRelativePositions(sourceComponent, anchor));
        if (cutter != null) {
            tag.putUUID(FallingTreeSubLevelData.FALLING_TREE_CUTTER_TAG, cutter.getUUID());
        }
        subLevel.setUserDataTag(tag);
    }

    private static List<Set<BlockPos>> attachedBlockComponents(final Set<BlockPos> attachedBlocks) {
        final Set<BlockPos> remaining = new HashSet<>(attachedBlocks);

        final List<Set<BlockPos>> components = new ArrayList<>();
        while (!remaining.isEmpty()) {
            final Set<BlockPos> component = new HashSet<>();
            final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            final BlockPos start = remaining.iterator().next();
            remaining.remove(start);
            queue.add(start);

            while (!queue.isEmpty()) {
                final BlockPos current = queue.removeFirst();
                component.add(current);
                for (final net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                    final BlockPos next = current.relative(direction);
                    if (remaining.remove(next)) {
                        queue.add(next);
                    }
                }
            }

            components.add(component);
        }

        return components;
    }
}
