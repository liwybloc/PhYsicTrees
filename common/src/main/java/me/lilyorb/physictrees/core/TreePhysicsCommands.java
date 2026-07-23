package me.lilyorb.physictrees.core;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class TreePhysicsCommands {
    private TreePhysicsCommands() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(Constants.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource()))));
    }

    private static int reload(final CommandSourceStack source) {
        try {
            TreePhysicsSettings.reload();
            source.sendSuccess(() -> Component.literal("Reloaded PhYsicTrees server config."), true);
            return 1;
        } catch (final RuntimeException exception) {
            source.sendFailure(Component.literal("Could not reload PhYsicTrees config: " + exception.getMessage()));
            return 0;
        }
    }
}
