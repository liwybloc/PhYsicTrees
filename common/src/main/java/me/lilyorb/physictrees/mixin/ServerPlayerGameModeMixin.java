package me.lilyorb.physictrees.mixin;

import me.lilyorb.physictrees.FallingTreeMining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Inject(method = "incrementDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void physictrees$applyFallingTreeBreakProgress(final BlockState state, final BlockPos pos, final int startTick, final CallbackInfoReturnable<Float> callback) {
        if (FallingTreeMining.isFallingTreeLog(this.level, pos, state)) {
            callback.setReturnValue(Math.min(1.0F, callback.getReturnValueF() + FallingTreeMining.partialBreakProgress()));
        }
    }
}
