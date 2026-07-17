package me.lilyorb.physictrees;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TreeResult {
    private final Set<BlockPos> logs = new HashSet<>();
    private final Set<BlockPos> leaves = new HashSet<>();
    private final Map<BlockPos, Integer> leafConnectionDistances = new HashMap<>();
    private boolean grounded;

    void addLog(final BlockPos pos) {
        logs.add(pos.immutable());
    }

    void addLeaf(final BlockPos pos) {
        leaves.add(pos.immutable());
    }

    void removeLeaf(final BlockPos pos) {
        leaves.remove(pos);
        leafConnectionDistances.remove(pos);
    }

    void setLeafConnectionDistance(final BlockPos pos, final int distance) {
        leafConnectionDistances.put(pos.immutable(), distance);
    }

    int leafConnectionDistance(final BlockPos pos) {
        return leafConnectionDistances.getOrDefault(pos, Integer.MAX_VALUE);
    }

    void markGrounded() {
        grounded = true;
    }

    public Set<BlockPos> logs() {
        return Collections.unmodifiableSet(logs);
    }

    public Set<BlockPos> leaves() {
        return Collections.unmodifiableSet(leaves);
    }

    public boolean isValid() {
        return grounded && !logs.isEmpty();
    }
}
