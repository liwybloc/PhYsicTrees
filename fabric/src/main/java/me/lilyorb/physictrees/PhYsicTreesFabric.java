package me.lilyorb.physictrees;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public final class PhYsicTreesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !TreeFelling.tryFell(level, player, pos, state));
        ServerTickEvents.END_SERVER_TICK.register(TreePhysics::tick);
    }
}
