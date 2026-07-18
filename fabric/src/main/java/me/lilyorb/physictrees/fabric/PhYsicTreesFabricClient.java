package me.lilyorb.physictrees.fabric;

import me.lilyorb.physictrees.client.particle.CollisionDustParticle;
import me.lilyorb.physictrees.particle.PhysicsParticles;
import me.lilyorb.physictrees.tree.TreeFelling;
import me.lilyorb.physictrees.tree.TreeResult;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static me.lilyorb.physictrees.core.Constants.*;

public final class PhYsicTreesFabricClient implements ClientModInitializer {

    private static BlockPos cachedTargetPos;
    private static long cachedTargetTick = Long.MIN_VALUE;
    private static TreeResult cachedTreeResult;

    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(PhysicsParticles.COLLISION_DUST, CollisionDustParticle.Provider::new);
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> renderAxeIndicator(guiGraphics));
    }

    private static void renderAxeIndicator(final GuiGraphics guiGraphics) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!shouldShowAxeIndicator(minecraft)) {
            return;
        }

        final int x = guiGraphics.guiWidth() / 2 + 12;
        final int y = guiGraphics.guiHeight() / 2 - 4;
        guiGraphics.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, ICON_OUTLINE);
        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, ICON_COLOR);
    }

    private static boolean shouldShowAxeIndicator(final Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null || minecraft.player.isShiftKeyDown()) {
            return false;
        }
        if (minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() != GameType.SURVIVAL) {
            return false;
        }
        if (!TreeFelling.hasValidHeldItem(minecraft.player)) {
            return false;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        final BlockPos pos = hitResult.getBlockPos();
        final long gameTime = minecraft.level.getGameTime();
        if (pos.equals(cachedTargetPos) && gameTime == cachedTargetTick) {
            return cachedTreeResult != null;
        }

        cachedTargetPos = pos.immutable();
        cachedTargetTick = gameTime;
        cachedTreeResult = TreeFelling.findTreeTarget(minecraft.level, pos);
        return cachedTreeResult != null;
    }
}
