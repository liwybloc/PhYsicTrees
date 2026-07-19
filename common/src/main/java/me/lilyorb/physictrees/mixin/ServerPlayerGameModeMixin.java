package me.lilyorb.physictrees.mixin;

import me.lilyorb.physictrees.physics.FallingTreeMining;
import me.lilyorb.physictrees.physics.TreePhysics;
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
import org.spongepowered.asm.mixin.Unique;
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

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void physictrees$seedFallingTreeStopValidation(final BlockPos pos, final ServerboundPlayerActionPacket.Action action, final Direction direction, final int maxBuildHeight, final int sequence, final CallbackInfo callback) {
        if (action != ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            return;
        }

        final BlockState state = this.level.getBlockState(pos);
        final float progress = FallingTreeMining.breakProgress(this.level, pos, state);
        if (progress <= 0.0F) {
            return;
        }

        this.destroyProgressStart = physictrees$seededStartTick(state, pos, this.destroyProgressStart, progress);
    }

    @Inject(method = "incrementDestroyProgress", at = @At("RETURN"))
    private void physictrees$keepCollisionDamageHiddenWhileMining(final BlockState state, final BlockPos pos, final int startTick, final CallbackInfoReturnable<Float> callback) {
        if (FallingTreeMining.breakProgress(this.level, pos, state) > 0.0F) {
            TreePhysics.suppressCollisionDamageWhileMining(this.level, pos);
        }
    }

    @Inject(method = "handleBlockBreakAction", at = @At("RETURN"))
    private void physictrees$handoffFallingTreeBreakProgress(final BlockPos pos, final ServerboundPlayerActionPacket.Action action, final Direction direction, final int maxBuildHeight, final int sequence, final CallbackInfo callback) {
        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK || action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            TreePhysics.stopSuppressingCollisionDamageWhileMining(this.level, pos);
            return;
        }

        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            return;
        }

        final BlockState state = this.level.getBlockState(pos);
        final float progress = FallingTreeMining.breakProgress(this.level, pos, state);
        if (progress <= 0.0F) {
            return;
        }

        TreePhysics.suppressCollisionDamageWhileMining(this.level, pos);
        this.destroyProgressStart = physictrees$seededStartTick(state, pos, this.destroyProgressStart, progress);
    }

    @Unique
    private int physictrees$seededStartTick(final BlockState state, final BlockPos pos, final int startTick, final float progress) {
        final float perTick = state.getDestroyProgress(this.player, this.player.level(), pos);
        if (progress <= 0.0F || perTick <= 0.0F) {
            return startTick;
        }

        final int seededTicks = Math.max(0, (int) Math.floor(progress / perTick) - 1);
        return Math.min(startTick, this.gameTicks - seededTicks);
    }
}
