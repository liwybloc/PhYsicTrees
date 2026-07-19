package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.core.TreePhysicsSettings;
import me.lilyorb.physictrees.particle.CollisionParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.Set;

@UtilityClass
public final class CollisionDamage {
    private static final int COLLISION_DAMAGE_PROGRESS_ID_SALT = 0x50540000;
    private static final Map<ServerSubLevel, FallingTreeImpact> IMPACTED_TREES = new HashMap<>();
    private static final Map<ServerSubLevel, Integer> COLLISION_DAMAGE_REFRESHES = new HashMap<>();
    private static final Map<ServerLevel, Set<BlockPos>> IMPACTED_LOG_POSITIONS = new IdentityHashMap<>();
    private static final Map<ServerLevel, Map<BlockPos, Integer>> ACTIVE_MINING_COLLISION_DAMAGE_SUPPRESSION = new IdentityHashMap<>();

    public static boolean isImpactedFallingTreeLog(final ServerLevel level, final BlockPos pos) {
        final Set<BlockPos> logs = IMPACTED_LOG_POSITIONS.get(level);
        return logs != null && logs.contains(pos);
    }

    public static boolean isCollisionDamageProgressPacket(final int id, final BlockPos pos) {
        return id == collisionDamageProgressId(pos);
    }

    public static Collection<FallingTreeImpact> impactedTrees() {
        return IMPACTED_TREES.values();
    }

    public static void trackImpacted(final FallingTreeImpact impact) {
        IMPACTED_TREES.put(impact.subLevel(), impact);
        cacheImpactedLogPositions(impact);
        COLLISION_DAMAGE_REFRESHES.put(impact.subLevel(), Math.max(1, TreePhysicsSettings.collisionDamageRefreshTicks()));
    }

    public static void suppressWhileMining(final ServerLevel level, final BlockPos pos) {
        if (!isImpactedFallingTreeLog(level, pos)) {
            return;
        }

        final BlockPos immutablePos = pos.immutable();
        ACTIVE_MINING_COLLISION_DAMAGE_SUPPRESSION.computeIfAbsent(level, key -> new HashMap<>())
                .put(immutablePos, 3);
        level.destroyBlockProgress(collisionDamageProgressId(immutablePos), immutablePos, -1);
    }

    public static void stopSuppressingWhileMining(final ServerLevel level, final BlockPos pos) {
        final Map<BlockPos, Integer> suppressed = ACTIVE_MINING_COLLISION_DAMAGE_SUPPRESSION.get(level);
        if (suppressed != null) {
            suppressed.remove(pos);
            if (suppressed.isEmpty()) {
                ACTIVE_MINING_COLLISION_DAMAGE_SUPPRESSION.remove(level);
            }
        }

        if (isImpactedFallingTreeLog(level, pos) && !level.getBlockState(pos).isAir()) {
            level.destroyBlockProgress(collisionDamageProgressId(pos), pos, collisionDamageStage());
        }
    }

    public static void refresh() {
        final Iterator<Map.Entry<ServerSubLevel, Integer>> iterator = COLLISION_DAMAGE_REFRESHES.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<ServerSubLevel, Integer> entry = iterator.next();
            final ServerSubLevel subLevel = entry.getKey();
            final int remainingTicks = entry.getValue();
            if (subLevel.isRemoved()) {
                final FallingTreeImpact impact = IMPACTED_TREES.remove(subLevel);
                removeImpactedLogPositions(impact);
                CollisionParticles.removeTrackedLogs(subLevel);
                clearCollisionDamage(impact);
                iterator.remove();
                continue;
            }

            final FallingTreeImpact impact = IMPACTED_TREES.get(subLevel);
            if (impact == null) {
                iterator.remove();
                continue;
            }

            if (remainingTicks <= 0) {
                clearCollisionDamage(impact);
                iterator.remove();
                continue;
            }

            showCollisionDamage(impact);
            entry.setValue(remainingTicks - 1);
        }
    }

    public static void tickMiningSuppression() {
        final Iterator<Map.Entry<ServerLevel, Map<BlockPos, Integer>>> levelIterator = ACTIVE_MINING_COLLISION_DAMAGE_SUPPRESSION.entrySet().iterator();
        while (levelIterator.hasNext()) {
            final Map<BlockPos, Integer> suppressed = levelIterator.next().getValue();
            final Iterator<Map.Entry<BlockPos, Integer>> posIterator = suppressed.entrySet().iterator();
            while (posIterator.hasNext()) {
                final Map.Entry<BlockPos, Integer> entry = posIterator.next();
                final int remainingTicks = entry.getValue() - 1;
                if (remainingTicks <= 0) {
                    posIterator.remove();
                } else {
                    entry.setValue(remainingTicks);
                }
            }

            if (suppressed.isEmpty()) {
                levelIterator.remove();
            }
        }
    }

    private static void cacheImpactedLogPositions(final FallingTreeImpact impact) {
        final Set<BlockPos> logs = IMPACTED_LOG_POSITIONS.computeIfAbsent(impact.level(), level -> new java.util.HashSet<>());
        logs.addAll(impact.logs());
    }

    private static void removeImpactedLogPositions(final FallingTreeImpact impact) {
        if (impact == null) {
            return;
        }

        final Set<BlockPos> logs = IMPACTED_LOG_POSITIONS.get(impact.level());
        if (logs == null) {
            return;
        }

        impact.logs().forEach(pos -> {
            logs.remove(pos);
            final Map<BlockPos, Integer> suppressed = ACTIVE_MINING_COLLISION_DAMAGE_SUPPRESSION.get(impact.level());
            if (suppressed != null) {
                suppressed.remove(pos);
                if (suppressed.isEmpty()) {
                    ACTIVE_MINING_COLLISION_DAMAGE_SUPPRESSION.remove(impact.level());
                }
            }
        });
        if (logs.isEmpty()) {
            IMPACTED_LOG_POSITIONS.remove(impact.level());
        }
    }

    private static void showCollisionDamage(final FallingTreeImpact impact) {
        final int progress = collisionDamageStage();
        impact.logs().forEach(pos -> {
            if (!isCollisionDamageSuppressedForMining(impact.level(), pos)) {
                impact.level().destroyBlockProgress(collisionDamageProgressId(pos), pos, progress);
            }
        });
    }

    private static void clearCollisionDamage(final FallingTreeImpact impact) {
        if (impact == null) {
            return;
        }

        impact.logs().forEach(pos -> impact.level().destroyBlockProgress(collisionDamageProgressId(pos), pos, -1));
    }

    private static boolean isCollisionDamageSuppressedForMining(final ServerLevel level, final BlockPos pos) {
        final Map<BlockPos, Integer> suppressed = ACTIVE_MINING_COLLISION_DAMAGE_SUPPRESSION.get(level);
        return suppressed != null && suppressed.containsKey(pos);
    }

    private static int collisionDamageStage() {
        return Math.clamp((int) Math.floor(TreePhysicsSettings.collisionBreakProgress() * 10.0D), 0, 9);
    }

    private static int collisionDamageProgressId(final BlockPos pos) {
        return COLLISION_DAMAGE_PROGRESS_ID_SALT ^ pos.hashCode();
    }
}
