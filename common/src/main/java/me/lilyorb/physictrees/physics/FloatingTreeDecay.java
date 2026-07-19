package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.platform.PhysicsPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

@UtilityClass
public final class FloatingTreeDecay {
    private static final String FLOATING_TREE_DECAY_MOD_ID = "floating_tree_decay";
    private static final String FLOATING_TREE_DECAY_PERSISTENT_LOG = "persistent_log";
    private static final String FLOATING_TREE_DECAY_STABLE_LOG = "stable_log";
    private static final String FLOATING_TREE_DECAY_DELAY = "decay_delay";

    public static void makeFallingTreeDecayable(final FallingTreeImpact impact) {
        if (!PhysicsPlatform.isModLoaded(FLOATING_TREE_DECAY_MOD_ID)) {
            return;
        }

        final ServerSubLevel subLevel = impact.subLevel();
        final ServerLevel level = subLevel.getLevel();
        final int decayDelay = floatingTreeDecayDelay();
        impact.logs().forEach(plotPos -> {
            final BlockState state = level.getBlockState(plotPos);
            BlockState decayableState = setPropertyByName(state, FLOATING_TREE_DECAY_PERSISTENT_LOG, false);
            decayableState = setPropertyByName(decayableState, FLOATING_TREE_DECAY_STABLE_LOG, false);
            decayableState = setPropertyByName(decayableState, FLOATING_TREE_DECAY_DELAY, decayDelay);
            if (decayableState != state) {
                level.setBlock(plotPos, decayableState, 2);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState setPropertyByName(final BlockState state, final String name, final Comparable value) {
        for (final Property property : state.getProperties()) {
            if (property.getName().equals(name) && property.getPossibleValues().contains(value)) {
                return state.setValue(property, value);
            }
        }
        return state;
    }

    private static int floatingTreeDecayDelay() {
        try {
            final Class<?> config = Class.forName("net.countered.floatingtreedecay.config.ModConfig");
            final Object value = config.getMethod("getTicksUntilDecay").invoke(null);
            if (value instanceof final Number number) {
                return Math.clamp(number.intValue(), 0, 100);
            }
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Floating Tree Decay is installed, but its config API could not be read.", exception);
        }

        throw new IllegalStateException("Floating Tree Decay getTicksUntilDecay did not return a number.");
    }
}
