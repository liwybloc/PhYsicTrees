package me.lilyorb.physictrees.mixin;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import me.lilyorb.physictrees.tree.TreeTags;
import me.lilyorb.physictrees.tree.TreeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FragileBlockCallback.class, remap = false)
public abstract class FragileBlockCallbackMixin {
    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void physictrees$silenceTreeLeafBreak(
            final ServerLevel level,
            final BlockPos pos,
            final BlockState state,
            final Vector3d hitPos,
            final CallbackInfoReturnable<BlockSubLevelCollisionCallback.CollisionResult> callback
    ) {
        if (!state.is(TreeTags.LEAVES) || !TreeUtil.isLeafIrresolute(state)) {
            return;
        }

        Block.dropResources(state, level, pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        callback.setReturnValue(new BlockSubLevelCollisionCallback.CollisionResult(JOMLConversion.ZERO, true));
    }
}
