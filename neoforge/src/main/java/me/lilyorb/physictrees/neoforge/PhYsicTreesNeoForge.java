package me.lilyorb.physictrees.neoforge;

import me.lilyorb.physictrees.core.Constants;
import me.lilyorb.physictrees.core.TreePhysicsCommands;
import me.lilyorb.physictrees.core.TreePhysicsSettings;
import me.lilyorb.physictrees.physics.TreePhysics;
import me.lilyorb.physictrees.tree.TreeFelling;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Constants.MOD_ID)
public final class PhYsicTreesNeoForge {
    public PhYsicTreesNeoForge(final IEventBus modEventBus) {
        TreePhysicsSettings.load();
        PhYsicTreesNeoForgeParticles.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(PhYsicTreesNeoForgeClient::registerParticles);
        }
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(PhYsicTreesNeoForgeClient::renderAxeIndicator);
        }
    }

    private void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level
                && TreeFelling.tryFell(level, event.getPlayer(), event.getPos(), event.getState())) {
            event.setCanceled(true);
        }
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        TreePhysicsCommands.register(event.getDispatcher());
    }

    private void onServerTick(final ServerTickEvent.Post event) {
        TreePhysics.tick(event.getServer());
    }
}
