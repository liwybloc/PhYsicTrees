package me.lilyorb.physictrees.mixin;

import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import me.lilyorb.physictrees.physics.AttachedBlockCollisionCallback;
import me.lilyorb.physictrees.physics.TreeLogCollisionCallback;
import me.lilyorb.physictrees.tree.TreeUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderBakery")
public abstract class RapierVoxelColliderBakeryMixin {
    @Redirect(
            method = "buildPhysicsDataForBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/block/BlockWithSubLevelCollisionCallback;sable$getCallback(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/api/physics/callback/BlockSubLevelCollisionCallback;"
            ),
            remap = false
    )
    private BlockSubLevelCollisionCallback physictrees$getLogCollisionCallback(final BlockState state) {
        if (TreeUtil.isLog(state)) {
            return TreeLogCollisionCallback.INSTANCE;
        }
        if (TreeUtil.isAttachedBlock(state)) {
            return AttachedBlockCollisionCallback.INSTANCE;
        }

        return BlockWithSubLevelCollisionCallback.sable$getCallback(state);
    }
}
