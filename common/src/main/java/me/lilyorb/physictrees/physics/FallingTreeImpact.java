package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;

public record FallingTreeImpact(ServerSubLevel subLevel, Set<BlockPos> logs, Set<BlockPos> leaves, Set<BlockPos> attachedBlocks) {
    public FallingTreeImpact {
        logs = Set.copyOf(logs);
        leaves = Set.copyOf(leaves);
        attachedBlocks = Set.copyOf(attachedBlocks);
    }

    public ServerLevel level() {
        return this.subLevel.getLevel();
    }
}
