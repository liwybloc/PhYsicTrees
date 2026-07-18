package me.lilyorb.physictrees.mixin;

import me.lilyorb.physictrees.FallingTreeMining;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    private int gameTicks;

    @Shadow
    private int destroyProgressStart;

    @Shadow
    private int lastSentState;

    @Inject(method = "incrementDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void physictrees$applyFallingTreeBreakProgress(final BlockState state, final BlockPos pos, final int startTick, final CallbackInfoReturnable<Float> callback) {
        if (FallingTreeMining.isFallingTreeLog(this.level, pos, state)) {
            callback.setReturnValue(Math.min(1.0F, callback.getReturnValueF() + FallingTreeMining.breakProgress(this.level, pos, state)));
        }
    }

    @Inject(method = "handleBlockBreakAction", at = @At("RETURN"))
    private void physictrees$seedFallingTreeStartProgress(final BlockPos pos, final ServerboundPlayerActionPacket.Action action, final Direction direction, final int maxBuildHeight, final int sequence, final CallbackInfo callback) {
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            return;
        }

        final BlockState state = this.level.getBlockState(pos);
        final float progress = FallingTreeMining.breakProgress(this.level, pos, state);
        if (progress <= 0.0F) {
            return;
        }

        final int stage = Math.clamp((int) Math.floor(progress * 10.0F), 0, 9);
        final float rawDestroyProgress = state.getDestroyProgress(this.player, this.player.level(), pos);
        if (rawDestroyProgress > 0.0F) {
            final int seededTicks = Math.max(0, (int) Math.floor(progress / rawDestroyProgress) - 1);
            this.destroyProgressStart = Math.min(this.destroyProgressStart, this.gameTicks - seededTicks);
        }

        this.lastSentState = Math.max(this.lastSentState, stage);
        this.level.destroyBlockProgress(this.player.getId(), pos, stage);
    }
}
