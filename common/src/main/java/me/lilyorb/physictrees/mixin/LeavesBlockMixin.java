package me.lilyorb.physictrees.mixin;

import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import me.lilyorb.physictrees.physics.LeafCollisionCallback;
import net.minecraft.world.level.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LeavesBlock.class, priority = 2000)
public abstract class LeavesBlockMixin implements BlockWithSubLevelCollisionCallback {
    @Override
    public BlockSubLevelCollisionCallback sable$getCallback() {
        return LeafCollisionCallback.INSTANCE;
    }
}
