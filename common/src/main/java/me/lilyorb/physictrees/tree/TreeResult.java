package me.lilyorb.physictrees.tree;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TreeResult {
    private final Set<BlockPos> logs = new HashSet<>();
    private final Set<BlockPos> leaves = new HashSet<>();
    private final Set<BlockPos> attachedBlocks = new HashSet<>();
    private final Map<BlockPos, Integer> leafConnectionDistances = new HashMap<>();
    private double totalMass;
    private boolean grounded;

    public void addLog(final BlockPos pos, final double mass) {
        if (logs.add(pos.immutable())) {
            totalMass += mass;
        }
    }

    public void addLeaf(final BlockPos pos, final double mass) {
        if (leaves.add(pos.immutable())) {
            totalMass += mass;
        }
    }

    public void addAttachedBlock(final BlockPos pos, final double mass) {
        if (attachedBlocks.add(pos.immutable())) {
            totalMass += mass;
        }
    }

    public void removeLeaf(final BlockPos pos, final double mass) {
        if (leaves.remove(pos)) {
            totalMass -= mass;
        }
        leafConnectionDistances.remove(pos);
    }

    public void setLeafConnectionDistance(final BlockPos pos, final int distance) {
        leafConnectionDistances.put(pos.immutable(), distance);
    }

    public int leafConnectionDistance(final BlockPos pos) {
        return leafConnectionDistances.getOrDefault(pos, Integer.MAX_VALUE);
    }

    public void markGrounded() {
        grounded = true;
    }

    public Set<BlockPos> logs() {
        return Collections.unmodifiableSet(logs);
    }

    public Set<BlockPos> leaves() {
        return Collections.unmodifiableSet(leaves);
    }

    public Set<BlockPos> attachedBlocks() {
        return Collections.unmodifiableSet(attachedBlocks);
    }

    public double totalMass() {
        return totalMass;
    }

    public boolean isValid() {
        return grounded && !logs.isEmpty();
    }
}
