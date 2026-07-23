package me.lilyorb.physictrees.fabric;

import me.lilyorb.physictrees.core.TreePhysicsCommands;
import me.lilyorb.physictrees.core.TreePhysicsSettings;
import me.lilyorb.physictrees.particle.PhysicsParticles;
import me.lilyorb.physictrees.physics.TreePhysics;
import me.lilyorb.physictrees.tree.TreeFelling;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public final class PhYsicTreesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TreePhysicsSettings.load();
        PhysicsParticles.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TreePhysicsCommands.register(dispatcher));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !TreeFelling.tryFell(level, player, pos, state));
        ServerTickEvents.END_SERVER_TICK.register(TreePhysics::tick);
    }
}
