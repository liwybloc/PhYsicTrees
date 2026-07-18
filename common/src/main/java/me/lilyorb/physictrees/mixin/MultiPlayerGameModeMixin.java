package me.lilyorb.physictrees.mixin;

import me.lilyorb.physictrees.FallingTreeMining;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private float destroyProgress;

    @Inject(method = "startDestroyBlock", at = @At("RETURN"))
    private void physictrees$seedStartDestroyProgress(final BlockPos pos, final Direction direction, final CallbackInfoReturnable<Boolean> callback) {
        seedDestroyProgress(pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("RETURN"))
    private void physictrees$seedContinueDestroyProgress(final BlockPos pos, final Direction direction, final CallbackInfoReturnable<Boolean> callback) {
        seedDestroyProgress(pos);
    }

    @Unique
    private void seedDestroyProgress(final BlockPos pos) {
        if (this.minecraft.level == null) {
            return;
        }

        final BlockState state = this.minecraft.level.getBlockState(pos);
        if (FallingTreeMining.isFallingTreeLog(this.minecraft.level, pos, state)) {
            this.destroyProgress = Math.max(this.destroyProgress, FallingTreeMining.clientBreakProgress(this.minecraft.level, pos, state));
        }
    }
}
