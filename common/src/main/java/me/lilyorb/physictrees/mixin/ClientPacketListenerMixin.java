package me.lilyorb.physictrees.mixin;

import me.lilyorb.physictrees.physics.FallingTreeMining;
import me.lilyorb.physictrees.physics.TreePhysics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleBlockDestruction", at = @At("HEAD"))
    private void physictrees$trackCollisionDamagedLogs(final ClientboundBlockDestructionPacket packet, final CallbackInfo callback) {
        if (TreePhysics.isCollisionDamageProgressPacket(packet.getId(), packet.getPos())) {
            FallingTreeMining.markClientCollisionDamaged(packet.getPos(), packet.getProgress() >= 0);
        }
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void physictrees$clearCollisionDamagedLogs(final CallbackInfo callback) {
        FallingTreeMining.clearClientCollisionDamage();
    }
}
