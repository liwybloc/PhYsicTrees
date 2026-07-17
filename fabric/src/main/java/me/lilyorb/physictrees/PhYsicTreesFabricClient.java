package me.lilyorb.physictrees;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class PhYsicTreesFabricClient implements ClientModInitializer {
    private static final int ICON_SIZE = 8;
    private static final int ICON_COLOR = 0xE0D18B2A;
    private static final int ICON_OUTLINE = 0xE02B1A08;

    private static BlockPos cachedTargetPos;
    private static long cachedTargetTick = Long.MIN_VALUE;
    private static TreeResult cachedTreeResult;

    @Override
    public void onInitializeClient() {
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
