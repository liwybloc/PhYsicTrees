package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

@UtilityClass
public final class FallingTreeSubLevelData {
    public static final String FALLING_TREE_LOGS_TAG = "physictrees_falling_tree_logs";
    public static final String FALLING_TREE_LEAVES_TAG = "physictrees_falling_tree_leaves";
    public static final String FALLING_TREE_ATTACHED_BLOCKS_TAG = "physictrees_falling_tree_attached_blocks";
    public static final String FALLING_TREE_CUTTER_TAG = "physictrees_falling_tree_cutter";

    private static final String FALLING_TREE_TAG = "physictrees_falling_tree";
    private static final String IMPACTED_TREE_TAG = "physictrees_impacted_tree";

    public static boolean isFallingTree(final SubLevel subLevel) {
        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final CompoundTag tag = serverSubLevel.getUserDataTag();
            return tag != null && tag.getBoolean(FALLING_TREE_TAG);
        }
        return subLevel != null;
    }

    public static boolean isImpactedFallingTree(final SubLevel subLevel) {
        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final CompoundTag tag = serverSubLevel.getUserDataTag();
            return tag != null && tag.getBoolean(FALLING_TREE_TAG) && tag.getBoolean(IMPACTED_TREE_TAG);
        }
        return false;
    }

    public static void markFallingTree(final ServerSubLevel subLevel, final Set<BlockPos> logs, final Set<BlockPos> leaves, final Set<BlockPos> attachedBlocks, final BlockPos cutPos, final Player player) {
        final CompoundTag tag = mutableTag(subLevel);
        tag.putBoolean(FALLING_TREE_TAG, true);
        tag.put(FALLING_TREE_LOGS_TAG, serializeRelativePositions(logs, cutPos));
        tag.put(FALLING_TREE_LEAVES_TAG, serializeRelativePositions(leaves, cutPos));
        tag.put(FALLING_TREE_ATTACHED_BLOCKS_TAG, serializeRelativePositions(attachedBlocks, cutPos));
        tag.putUUID(FALLING_TREE_CUTTER_TAG, player.getUUID());
        subLevel.setUserDataTag(tag);
    }

    public static void markImpacted(final ServerSubLevel subLevel) {
        final CompoundTag tag = mutableTag(subLevel);
        tag.putBoolean(IMPACTED_TREE_TAG, true);
        subLevel.setUserDataTag(tag);
    }

    public static FallingTreeImpact impactSnapshot(final ServerSubLevel subLevel) {
        return new FallingTreeImpact(
                subLevel,
                storedRelativePositions(subLevel, FALLING_TREE_LOGS_TAG),
                storedRelativePositions(subLevel, FALLING_TREE_LEAVES_TAG),
                storedRelativePositions(subLevel, FALLING_TREE_ATTACHED_BLOCKS_TAG)
        );
    }

    public static CompoundTag mutableTag(final ServerSubLevel subLevel) {
        return subLevel.getUserDataTag() == null ? new CompoundTag() : subLevel.getUserDataTag();
    }

    public static Player getCutter(final ServerLevel level, final ServerSubLevel subLevel) {
        final CompoundTag tag = subLevel.getUserDataTag();
        if (tag == null || !tag.hasUUID(FALLING_TREE_CUTTER_TAG)) {
            return null;
        }

        final ServerPlayer player = level.getServer().getPlayerList().getPlayer(tag.getUUID(FALLING_TREE_CUTTER_TAG));
        return player != null && player.level() == level ? player : null;
    }

    public static void forEachStoredLogPosition(final ServerSubLevel subLevel, final Consumer<BlockPos> consumer) {
        forEachStoredRelativePosition(subLevel, FALLING_TREE_LOGS_TAG, consumer);
    }

    public static void forEachStoredLeafPosition(final ServerSubLevel subLevel, final Consumer<BlockPos> consumer) {
        forEachStoredRelativePosition(subLevel, FALLING_TREE_LEAVES_TAG, consumer);
    }

    public static void forEachStoredAttachedBlockPosition(final ServerSubLevel subLevel, final Consumer<BlockPos> consumer) {
        forEachStoredRelativePosition(subLevel, FALLING_TREE_ATTACHED_BLOCKS_TAG, consumer);
    }

    public static void forEachStoredRelativePosition(final ServerSubLevel subLevel, final String tagName, final Consumer<BlockPos> consumer) {
        for (final BlockPos pos : storedRelativePositions(subLevel, tagName)) {
            consumer.accept(pos);
        }
    }

    private static Set<BlockPos> storedRelativePositions(final ServerSubLevel subLevel, final String tagName) {
        final Set<BlockPos> result = new HashSet<>();
        final CompoundTag tag = subLevel.getUserDataTag();
        if (tag == null || !tag.contains(tagName)) {
            return result;
        }

        final BlockPos plotCenter = subLevel.getPlot().getCenterBlock();
        final ListTag positions = tag.getList(tagName, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < positions.size(); index++) {
            final CompoundTag posTag = positions.getCompound(index);
            final BlockPos relativePos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
            result.add(plotCenter.offset(relativePos));
        }
        return result;
    }

    public static ListTag serializeRelativePositions(final Set<BlockPos> positions, final BlockPos anchor) {
        final ListTag serializedPositions = new ListTag();
        for (final BlockPos pos : positions) {
            final BlockPos relativePos = pos.subtract(anchor);
            final CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", relativePos.getX());
            posTag.putInt("y", relativePos.getY());
            posTag.putInt("z", relativePos.getZ());
            serializedPositions.add(posTag);
        }
        return serializedPositions;
    }

    public static BoundingBox3i boundsFor(final Set<BlockPos> blocks) {
        final Iterator<BlockPos> iterator = blocks.iterator();
        final BlockPos first = iterator.next();
        int minX = first.getX();
        int minY = first.getY();
        int minZ = first.getZ();
        int maxX = first.getX();
        int maxY = first.getY();
        int maxZ = first.getZ();

        while (iterator.hasNext()) {
            final BlockPos pos = iterator.next();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new BoundingBox3i(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static Vector3d blockCenter(final BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }
}
