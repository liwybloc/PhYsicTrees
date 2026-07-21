package me.lilyorb.physictrees.physics;

import me.lilyorb.physictrees.core.Constants;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.joml.Vector3d;

public final class TreePhysicsSounds {
    public static final Holder<SoundEvent> TREE_CREAK = create("sub_level.tree.creak");
    public static final Holder<SoundEvent> TREE_IMPACT = create("sub_level.tree.impact");
    public static final Holder<SoundEvent> LEAF_RUSTLE = create("sub_level.tree.leaf_rustle");

    private TreePhysicsSounds() {
    }

    private static Holder<SoundEvent> create(final String id) {
        return Holder.direct(SoundEvent.createVariableRangeEvent(Constants.id(id)));
    }

    public static void playCreak(final ServerLevel level, final ServerSubLevel subLevel) {
        if (FallingTreeSubLevelData.hasPlayedCreak(subLevel)) {
            return;
        }

        final Vector3d position = subLevel.logicalPose().position();
        final float pitch = pitch(subLevel);
        level.playSound(null, position.x, position.y, position.z, TREE_CREAK, SoundSource.BLOCKS, 1.0F, pitch);
        if (FallingTreeSubLevelData.hasStoredLeaves(subLevel)) {
            level.playSound(null, position.x, position.y, position.z, LEAF_RUSTLE, SoundSource.BLOCKS, 0.8F, pitch);
        }
        FallingTreeSubLevelData.markPlayedCreak(subLevel);
    }

    public static void playImpact(final ServerLevel level, final ServerSubLevel subLevel) {
        if (FallingTreeSubLevelData.hasPlayedImpact(subLevel)) {
            return;
        }

        final Vector3d position = subLevel.logicalPose().position();
        level.playSound(null, position.x, position.y, position.z, TREE_IMPACT, SoundSource.BLOCKS, 3.0F, pitch(subLevel));
        FallingTreeSubLevelData.markPlayedImpact(subLevel);
    }

    private static float pitch(final ServerSubLevel subLevel) {
        return (float) (1.0D - Mth.clamp(FallingTreeSubLevelData.storedLogCount(subLevel) / 64.0D, 0.0D, 0.25D));
    }
}
