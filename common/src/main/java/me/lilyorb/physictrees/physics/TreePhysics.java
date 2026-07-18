package me.lilyorb.physictrees.physics;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import me.lilyorb.physictrees.particle.CollisionDustParticleOptions;
import me.lilyorb.physictrees.platform.PhysicsPlatform;
import me.lilyorb.physictrees.tree.TreeResult;
import me.lilyorb.physictrees.tree.TreeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.*;

public final class TreePhysics {
    private static final String FALLING_TREE_TAG = "physictrees_falling_tree";
    private static final String IMPACTED_TREE_TAG = "physictrees_impacted_tree";
    private static final String FALLING_TREE_LOGS_TAG = "physictrees_falling_tree_logs";
    private static final String FLOATING_TREE_DECAY_MOD_ID = "floating_tree_decay";
    private static final String FLOATING_TREE_DECAY_PERSISTENT_LOG = "persistent_log";
    private static final String FLOATING_TREE_DECAY_STABLE_LOG = "stable_log";
    private static final String FLOATING_TREE_DECAY_DELAY = "decay_delay";
    private static final int COLLISION_DAMAGE_PROGRESS_ID_SALT = 0x50540000;
    private static final List<PendingFallForce> PENDING_FALL_FORCES = new ArrayList<>();
    private static final List<PendingBlockImpactParticle> PENDING_BLOCK_IMPACT_PARTICLES = new ArrayList<>();
    private static final Map<ServerSubLevel, Integer> COLLISION_DAMAGE_REFRESHES = new HashMap<>();
    private static final Map<ServerSubLevel, Set<BlockPos>> IMPACT_PARTICLE_LOGS = new HashMap<>();
    private static final Map<ServerLevel, Set<BlockPos>> IMPACTED_LOG_POSITIONS = new IdentityHashMap<>();

    private TreePhysics() {
    }

    public static boolean spawnFallingTree(final ServerLevel level, final Player player, final BlockPos cutPos, final TreeResult tree) {
        final Set<BlockPos> logs = new HashSet<>(tree.logs());
        if (TreePhysicsSettings.breakCutBlock()) {
            logs.remove(cutPos);
        }

        final Set<BlockPos> blocks = new HashSet<>(tree.logs().size() + tree.leaves().size());
        blocks.addAll(logs);
        blocks.addAll(tree.leaves());
        if (blocks.isEmpty()) {
            return false;
        }

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, cutPos, blocks, new BoundingBox3i(cutPos, cutPos));
        if (subLevel == null) {
            return false;
        }

        markFallingTreeSubLevel(subLevel, logs, cutPos);
        clearOriginalBlocks(level, blocks);
        if (TreePhysicsSettings.breakCutBlock()) {
            breakCutBlock(level, player, cutPos);
        }
        queueFallForce(level, player, cutPos, logs, tree, subLevel);
        return true;
    }

    public static boolean isFallingTreeSubLevel(final SubLevel subLevel) {
        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final CompoundTag tag = serverSubLevel.getUserDataTag();
            return tag != null && tag.getBoolean(FALLING_TREE_TAG);
        }
        return subLevel != null;
    }

    public static boolean isImpactedFallingTreeSubLevel(final SubLevel subLevel) {
        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final CompoundTag tag = serverSubLevel.getUserDataTag();
            return tag != null && tag.getBoolean(FALLING_TREE_TAG) && tag.getBoolean(IMPACTED_TREE_TAG);
        }
        return false;
    }

    public static boolean isImpactedFallingTreeLog(final ServerLevel level, final BlockPos pos) {
        final Set<BlockPos> logs = IMPACTED_LOG_POSITIONS.get(level);
        return logs != null && logs.contains(pos);
    }

    public static void markFallingTreeImpacted(final ServerSubLevel subLevel) {
        if (!isFallingTreeSubLevel(subLevel) || isImpactedFallingTreeSubLevel(subLevel)) {
            return;
        }

        final CompoundTag tag = subLevel.getUserDataTag() == null ? new CompoundTag() : subLevel.getUserDataTag();
        tag.putBoolean(IMPACTED_TREE_TAG, true);
        subLevel.setUserDataTag(tag);
        cacheImpactedLogPositions(subLevel);
        makeFallingTreeDecayable(subLevel);
        COLLISION_DAMAGE_REFRESHES.put(subLevel, Math.max(1, TreePhysicsSettings.collisionDamageRefreshTicks()));
    }

    public static void queueBlockImpactParticles(final ServerLevel level, final ServerSubLevel subLevel, final BlockPos logPos, final Vector3d hitPos, final BlockState collidedState, final BlockPos collidedPos) {
        if (!isFallingTreeSubLevel(subLevel) || subLevel.isRemoved()) {
            return;
        }

        final Set<BlockPos> impactedLogs = IMPACT_PARTICLE_LOGS.computeIfAbsent(subLevel, key -> new HashSet<>());
        final BlockPos immutableLogPos = logPos.immutable();
        if (impactedLogs.add(immutableLogPos)) {
            final Vector3d logCenter = new Vector3d(logPos.getX() + 0.5D, logPos.getY() + 0.5D, logPos.getZ() + 0.5D);
            final Vector3d worldLogCenter = Sable.HELPER.projectOutOfSubLevel(level, logCenter, new Vector3d());
            PENDING_BLOCK_IMPACT_PARTICLES.add(new PendingBlockImpactParticle(level, worldLogCenter, new CollisionDustParticleOptions(collidedState, collidedPos.immutable())));
        }
    }

    public static void tick(final MinecraftServer server) {
        final Iterator<PendingFallForce> iterator = PENDING_FALL_FORCES.iterator();
        while (iterator.hasNext()) {
            final PendingFallForce force = iterator.next();
            if (force.subLevel.isRemoved()) {
                iterator.remove();
                continue;
            }

            applyForceStep(force);
            force.remainingTicks--;
            if (force.remainingTicks <= 0) {
                iterator.remove();
            }
        }

        refreshCollisionDamage();
        queueContactingLogImpactParticles();
        flushBlockImpactParticles();
    }

    private static void clearOriginalBlocks(final ServerLevel level, final Set<BlockPos> blocks) {
        final BlockState barrier = Blocks.BARRIER.defaultBlockState();
        final BlockState air = Blocks.AIR.defaultBlockState();
        for (final BlockPos pos : blocks) {
            level.setBlock(pos, barrier, 2);
            level.setBlock(pos, air, 2);
        }
    }

    private static void breakCutBlock(final ServerLevel level, final Player player, final BlockPos cutPos) {
        final BlockState state = level.getBlockState(cutPos);
        Block.dropResources(state, level, cutPos, level.getBlockEntity(cutPos), player, player.getMainHandItem());
        level.setBlock(cutPos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void makeFallingTreeDecayable(final ServerSubLevel subLevel) {
        if (!PhysicsPlatform.isModLoaded(FLOATING_TREE_DECAY_MOD_ID)) {
            return;
        }

        final ServerLevel level = subLevel.getLevel();
        final int decayDelay = floatingTreeDecayDelay();
        forEachStoredLogPosition(subLevel, plotPos -> {
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

    private static void markFallingTreeSubLevel(final ServerSubLevel subLevel, final Set<BlockPos> logs, final BlockPos cutPos) {
        final CompoundTag tag = subLevel.getUserDataTag() == null ? new CompoundTag() : subLevel.getUserDataTag();
        tag.putBoolean(FALLING_TREE_TAG, true);
        tag.put(FALLING_TREE_LOGS_TAG, serializeSubLevelLogPositions(subLevel, logs, cutPos));
        subLevel.setUserDataTag(tag);
    }

    private static ListTag serializeSubLevelLogPositions(final ServerSubLevel subLevel, final Set<BlockPos> logs, final BlockPos cutPos) {
        final ListTag serializedLogs = new ListTag();
        for (final BlockPos log : logs) {
            final BlockPos plotPos = log.subtract(cutPos);
            final CompoundTag logTag = new CompoundTag();
            logTag.putInt("x", plotPos.getX());
            logTag.putInt("y", plotPos.getY());
            logTag.putInt("z", plotPos.getZ());
            serializedLogs.add(logTag);
        }
        return serializedLogs;
    }

    private static void refreshCollisionDamage() {
        final Iterator<Map.Entry<ServerSubLevel, Integer>> iterator = COLLISION_DAMAGE_REFRESHES.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<ServerSubLevel, Integer> entry = iterator.next();
            final ServerSubLevel subLevel = entry.getKey();
            final int remainingTicks = entry.getValue();
            if (subLevel.isRemoved()) {
                removeImpactedLogPositions(subLevel);
                IMPACT_PARTICLE_LOGS.remove(subLevel);
                clearCollisionDamage(subLevel);
                iterator.remove();
                continue;
            }

            if (remainingTicks <= 0) {
                clearCollisionDamage(subLevel);
                iterator.remove();
                continue;
            }

            showCollisionDamage(subLevel);
            entry.setValue(remainingTicks - 1);
        }
    }

    private static void flushBlockImpactParticles() {
        final Iterator<PendingBlockImpactParticle> iterator = PENDING_BLOCK_IMPACT_PARTICLES.iterator();
        while (iterator.hasNext()) {
            final PendingBlockImpactParticle particle = iterator.next();
            iterator.remove();
            particle.level.sendParticles(
                    particle.options,

                    particle.pos.x,
                    particle.pos.y + TreePhysicsSettings.collisionParticleYOffset(),
                    particle.pos.z,
                    Math.max(0, TreePhysicsSettings.collisionParticleCount()),
                    TreePhysicsSettings.collisionParticleOffsetX(),
                    TreePhysicsSettings.collisionParticleOffsetY(),
                    TreePhysicsSettings.collisionParticleOffsetZ(),
                    TreePhysicsSettings.collisionParticleSpeed()
            );
        }
    }

    private static void queueContactingLogImpactParticles() {
        for (final ServerSubLevel subLevel : new ArrayList<>(COLLISION_DAMAGE_REFRESHES.keySet())) {
            if (subLevel.isRemoved()) {
                continue;
            }

            final ServerLevel level = subLevel.getLevel();
            forEachStoredLogPosition(subLevel, plotPos -> {
                final Set<BlockPos> impactedLogs = IMPACT_PARTICLE_LOGS.get(subLevel);
                if (impactedLogs != null && impactedLogs.contains(plotPos)) {
                    return;
                }

                final Vector3d worldLogCenter = Sable.HELPER.projectOutOfSubLevel(level, blockCenter(plotPos), new Vector3d());
                final BlockContact contact = findBlockContact(level, worldLogCenter);
                if (contact != null) {
                    queueBlockImpactParticles(level, subLevel, plotPos, worldLogCenter, contact.state(), contact.pos());
                }
            });
        }
    }

    private static BlockContact findBlockContact(final ServerLevel level, final Vector3d worldLogCenter) {
        final double probeDistance = TreePhysicsSettings.impactProbeDistance();
        final double[][] probes = {
                {0.0D, -probeDistance, 0.0D},
                {probeDistance, 0.0D, 0.0D},
                {-probeDistance, 0.0D, 0.0D},
                {0.0D, 0.0D, probeDistance},
                {0.0D, 0.0D, -probeDistance},
                {0.0D, probeDistance, 0.0D}
        };

        for (final double[] probe : probes) {
            final BlockPos pos = BlockPos.containing(worldLogCenter.x + probe[0], worldLogCenter.y + probe[1], worldLogCenter.z + probe[2]);
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            final BlockState state = level.getBlockState(pos);
            if (isValidImpactSurface(state)) {
                return new BlockContact(state, pos.immutable());
            }
        }

        return null;
    }

    private static boolean isValidImpactSurface(final BlockState state) {
        return !state.isAir() && !TreeUtil.isLeaf(state) && !TreeUtil.isLog(state) && state.isSolid();
    }

    private static void cacheImpactedLogPositions(final ServerSubLevel subLevel) {
        final Set<BlockPos> logs = IMPACTED_LOG_POSITIONS.computeIfAbsent(subLevel.getLevel(), level -> new HashSet<>());
        forEachStoredLogPosition(subLevel, logs::add);
    }

    private static void removeImpactedLogPositions(final ServerSubLevel subLevel) {
        final Set<BlockPos> logs = IMPACTED_LOG_POSITIONS.get(subLevel.getLevel());
        if (logs == null) {
            return;
        }

        forEachStoredLogPosition(subLevel, logs::remove);
        if (logs.isEmpty()) {
            IMPACTED_LOG_POSITIONS.remove(subLevel.getLevel());
        }
    }

    private static void showCollisionDamage(final ServerSubLevel subLevel) {
        final int progress = Math.clamp((int) Math.floor(TreePhysicsSettings.collisionBreakProgress() * 10.0D), 0, 9);
        forEachStoredLogPosition(subLevel, pos -> subLevel.getLevel().destroyBlockProgress(collisionDamageProgressId(subLevel, pos), pos, progress));
    }

    private static void clearCollisionDamage(final ServerSubLevel subLevel) {
        forEachStoredLogPosition(subLevel, pos -> subLevel.getLevel().destroyBlockProgress(collisionDamageProgressId(subLevel, pos), pos, -1));
    }

    private static void forEachStoredLogPosition(final ServerSubLevel subLevel, final java.util.function.Consumer<BlockPos> consumer) {
        final CompoundTag tag = subLevel.getUserDataTag();
        if (tag == null || !tag.contains(FALLING_TREE_LOGS_TAG)) {
            return;
        }

        final BlockPos plotCenter = subLevel.getPlot().getCenterBlock();
        final ListTag logs = tag.getList(FALLING_TREE_LOGS_TAG, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < logs.size(); index++) {
            final CompoundTag logTag = logs.getCompound(index);
            final BlockPos relativePos = new BlockPos(logTag.getInt("x"), logTag.getInt("y"), logTag.getInt("z"));
            final BlockPos plotPos = plotCenter.offset(relativePos);
            consumer.accept(plotPos);
        }
    }

    private static int collisionDamageProgressId(final ServerSubLevel subLevel, final BlockPos pos) {
        return COLLISION_DAMAGE_PROGRESS_ID_SALT ^ pos.hashCode();
    }

    private static void queueFallForce(final ServerLevel level, final Player player, final BlockPos cutPos, final Set<BlockPos> logs, final TreeResult tree, final ServerSubLevel subLevel) {
        final Vec3 sideFromPlayerToCut = cutPos.getCenter().subtract(player.getEyePosition()).multiply(1.0D, 0.0D, 1.0D);
        final Vec3 fallbackSide = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        final Vec3 cutSide = snapToCardinal(chooseCutSide(sideFromPlayerToCut, fallbackSide));
        final Vector3d fallDirection = new Vector3d(cutSide.x, 0.0D, cutSide.z).normalize();

        final Vector3d topImpulsePosition = findTopImpulsePoint(logs, cutPos, subLevel);
        final Vector3d baseImpulsePosition = toSubLevelPosition(cutPos, cutPos, subLevel);
        final double forceScale = getForceScale(tree);
        final int forceTicks = Math.max(1, TreePhysicsSettings.forceTicks());
        final double angularNudge = TreePhysicsSettings.baseAngularNudge() + forceScale;
        final double upwardNudge = TreePhysicsSettings.breakCutBlock() ? 0.0D : TreePhysicsSettings.baseUpwardNudge() * forceScale;
        final Vector3d topImpulse = new Vector3d(fallDirection).mul(angularNudge / forceTicks);
        topImpulse.y += upwardNudge / forceTicks;
        final Vector3d baseCounterImpulse = new Vector3d(fallDirection)
                .mul(-angularNudge *
                        (TreePhysicsSettings.breakCutBlock() ? TreePhysicsSettings.counterImpulseBreakRatio()
                                : TreePhysicsSettings.counterImpulseNoBreakRatio()) / forceTicks);

        final PendingFallForce force = new PendingFallForce(level, subLevel, topImpulsePosition, topImpulse, baseImpulsePosition, baseCounterImpulse, forceTicks);
        applyForceStep(force);
        force.remainingTicks--;
        if (force.remainingTicks > 0) {
            PENDING_FALL_FORCES.add(force);
        }
    }

    private static double getForceScale(final TreeResult tree) {
        return tree.logs().size() * TreePhysicsSettings.logForceAmplitude() + tree.leaves().size() * TreePhysicsSettings.leafForceAmplitude();
    }

    private static Vector3d blockCenter(final BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static void applyForceStep(final PendingFallForce force) {
        final RigidBodyHandle handle = SubLevelPhysicsSystem.get(force.level).getPhysicsHandle(force.subLevel);
        handle.applyImpulseAtPoint(new Vector3d(force.topImpulsePosition), new Vector3d(force.topImpulse));
        handle.applyImpulseAtPoint(new Vector3d(force.baseImpulsePosition), new Vector3d(force.baseCounterImpulse));
    }

    private static Vector3d findTopImpulsePoint(final Set<BlockPos> logs, final BlockPos cutPos, final ServerSubLevel subLevel) {
        BlockPos highest = cutPos;
        for (final BlockPos pos : logs) {
            if (pos.getY() > highest.getY()) {
                highest = pos;
            }
        }

        return toSubLevelPosition(highest, cutPos, subLevel);
    }

    private static Vector3d toSubLevelPosition(final BlockPos worldPos, final BlockPos cutPos, final ServerSubLevel subLevel) {
        final Vector3d worldOffsetFromCut = blockCenter(worldPos).sub(blockCenter(cutPos), new Vector3d());
        return blockCenter(subLevel.getPlot().getCenterBlock()).add(worldOffsetFromCut);
    }

    private static Vec3 chooseCutSide(final Vec3 sideFromCutToPlayer, final Vec3 fallbackSide) {
        if (sideFromCutToPlayer.lengthSqr() > 1.0E-5D) {
            return sideFromCutToPlayer.normalize();
        }
        if (fallbackSide.lengthSqr() > 1.0E-5D) {
            return fallbackSide.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static Vec3 snapToCardinal(final Vec3 direction) {
        if (Math.abs(direction.x) >= Math.abs(direction.z)) {
            return new Vec3(Math.signum(direction.x), 0.0D, 0.0D);
        }
        return new Vec3(0.0D, 0.0D, Math.signum(direction.z));
    }

    private static final class PendingFallForce {
        private final ServerLevel level;
        private final ServerSubLevel subLevel;
        private final Vector3d topImpulsePosition;
        private final Vector3d topImpulse;
        private final Vector3d baseImpulsePosition;
        private final Vector3d baseCounterImpulse;
        private int remainingTicks;

        private PendingFallForce(final ServerLevel level, final ServerSubLevel subLevel, final Vector3d topImpulsePosition, final Vector3d topImpulse, final Vector3d baseImpulsePosition, final Vector3d baseCounterImpulse, final int remainingTicks) {
            this.level = level;
            this.subLevel = subLevel;
            this.topImpulsePosition = topImpulsePosition;
            this.topImpulse = topImpulse;
            this.baseImpulsePosition = baseImpulsePosition;
            this.baseCounterImpulse = baseCounterImpulse;
            this.remainingTicks = remainingTicks;
        }
    }

    private record PendingBlockImpactParticle(ServerLevel level, Vector3d pos, CollisionDustParticleOptions options) {
    }

    private record BlockContact(BlockState state, BlockPos pos) {
    }
}
