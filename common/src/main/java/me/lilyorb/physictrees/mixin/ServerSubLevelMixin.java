package me.lilyorb.physictrees.mixin;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import me.lilyorb.physictrees.physics.TreePhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerSubLevel.class)
public abstract class ServerSubLevelMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;tick()V"
            ),
            remap = false
    )
    private void physictrees$skipSableSplitterForManagedSubLevels(final SubLevelHeatMapManager manager) {
        final ServerSubLevel self = (ServerSubLevel) (Object) this;
        if (TreePhysics.isFallingTreeSubLevel(self) || TreePhysics.isDetachedAccessorySubLevel(self)) {
            return;
        }

        manager.tick();
    }
}
