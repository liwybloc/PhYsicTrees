package me.lilyorb.physictrees.neoforge;

import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.client.particle.CollisionDustParticle;
import me.lilyorb.physictrees.particle.PhysicsParticles;
import me.lilyorb.physictrees.tree.TreeFelling;
import me.lilyorb.physictrees.tree.TreeResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import static me.lilyorb.physictrees.core.Constants.*;

@UtilityClass
public final class PhYsicTreesNeoForgeClient {

    private static BlockPos cachedTargetPos;
    private static long cachedTargetTick = Long.MIN_VALUE;
    private static TreeResult cachedTreeResult;

    public static void registerParticles(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(PhysicsParticles.COLLISION_DUST, CollisionDustParticle.Provider::new);
    }

    public static void renderAxeIndicator(final RenderGuiEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!shouldShowAxeIndicator(minecraft)) {
            return;
        }

        final GuiGraphics guiGraphics = event.getGuiGraphics();
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
