package me.lilyorb.physictrees.core;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

public final class TreePhysicsSettings {
    private static boolean registered;

    private TreePhysicsSettings() {
    }

    public static void load() {
        if (!registered) {
            AutoConfig.register(Values.class, Toml4jConfigSerializer::new);
            registered = true;
        }
    }

    public static void reload() {
        load();
        AutoConfig.getConfigHolder(Values.class).load();
    }

    public static boolean breakCutBlock() {
        return values().break_cut_block;
    }

    public static boolean allowLowerBlockFelling() {
        return values().allow_lower_block_felling;
    }

    public static boolean leafDecay() {
        return values().leaf_decay;
    }

    public static double leafDecayTicks() {
        return values().leaf_decay_ticks;
    }

    public static double leafDecayTicksRandom() {
        return values().leaf_decay_ticks_random;
    }

    public static double partialBreakProgress() {
        return values().partial_break_progress;
    }

    public static double collisionBreakProgress() {
        return values().collision_break_progress;
    }

    public static int collisionDamageRefreshTicks() {
        return values().collision_damage_refresh_ticks;
    }

    public static int forceTicks() {
        return values().force_ticks;
    }

    public static double getAngularMultiplier() {
        return values().angular_multiplier;
    }

    public static double centerMassImpulseRatio() {
        return values().center_mass_impulse_ratio;
    }

    public static double targetFallAngularVelocity() {
        return values().target_fall_angular_velocity;
    }

    public static double targetFallLinearVelocity() {
        return values().target_fall_linear_velocity;
    }

    public static double forwardFallVelocityNudge() {
        return values().forward_fall_velocity_nudge;
    }

    public static double downwardFallVelocityNudge() {
        return values().downward_fall_velocity_nudge;
    }

    public static double fallAngularVelocityHeightExponent() {
        return values().fall_angular_velocity_height_exponent;
    }

    public static double fallLinearVelocityLogExponent() {
        return values().fall_linear_velocity_log_exponent;
    }

    public static double forwardFallVelocityLogExponent() {
        return values().forward_fall_velocity_log_exponent;
    }

    public static double referenceForceLogCount() {
        return values().reference_force_log_count;
    }

    public static double referenceForceHeight() {
        return values().reference_force_height;
    }

    public static double impactProbeDistance() {
        return values().impact_probe_distance;
    }

    public static int collisionParticleCount() {
        return values().collision_particle_count;
    }

    public static double collisionParticleOffsetX() {
        return values().collision_particle_offset_x;
    }

    public static double collisionParticleOffsetY() {
        return values().collision_particle_offset_y;
    }

    public static double collisionParticleOffsetZ() {
        return values().collision_particle_offset_z;
    }

    public static double collisionParticleSpeed() {
        return values().collision_particle_speed;
    }

    public static double collisionParticleYOffset() {
        return values().collision_particle_y_offset;
    }

    public static float collisionParticleGravity() {
        return values().collision_particle_gravity;
    }

    public static int collisionParticleLifetime() {
        return values().collision_particle_lifetime;
    }

    public static int collisionParticleLifetimeRandom() {
        return values().collision_particle_lifetime_random;
    }

    public static float collisionParticleAlpha() {
        return values().collision_particle_alpha;
    }

    public static float collisionParticleSize() {
        return values().collision_particle_size;
    }

    public static double collisionParticleRandomXzSpeed() {
        return values().collision_particle_random_xz_speed;
    }

    public static double collisionParticleUpwardSpeed() {
        return values().collision_particle_upward_speed;
    }

    public static double collisionParticleRandomUpwardSpeed() {
        return values().collision_particle_random_upward_speed;
    }

    private static Values values() {
        load();
        return AutoConfig.getConfigHolder(Values.class).getConfig();
    }

    @Config(name = "physictrees-server")
    public static final class Values implements ConfigData {
        public boolean break_cut_block = true;
        public boolean allow_lower_block_felling = false;
        public boolean leaf_decay = true;
        public double leaf_decay_ticks = 20.0D;
        public double leaf_decay_ticks_random = 10.0D;
        public double partial_break_progress = 0.2D;
        public double collision_break_progress = 0.5D;
        public int collision_damage_refresh_ticks = 20 * 120;
        public int force_ticks = 2;
        public double angular_multiplier = 0.75D;
        public double center_mass_impulse_ratio = 0.3D;
        public double target_fall_angular_velocity = 0.85D;
        public double target_fall_linear_velocity = 0.12D;
        public double forward_fall_velocity_nudge = 0.15D;
        public double downward_fall_velocity_nudge = 0.35D;
        public double fall_angular_velocity_height_exponent = -0.15D;
        public double fall_linear_velocity_log_exponent = -0.25D;
        public double forward_fall_velocity_log_exponent = 0.85D;
        public double reference_force_log_count = 4.0D;
        public double reference_force_height = 6.0D;
        public double impact_probe_distance = 0.55D;
        public int collision_particle_count = 8;
        public double collision_particle_offset_x = 0.25D;
        public double collision_particle_offset_y = 0.1D;
        public double collision_particle_offset_z = 0.25D;
        public double collision_particle_speed = 0.0D;
        public double collision_particle_y_offset = -0.3D;
        public float collision_particle_gravity = -0.003F;
        public int collision_particle_lifetime = 20;
        public int collision_particle_lifetime_random = 10;
        public float collision_particle_alpha = 0.75F;
        public float collision_particle_size = 3.2F;
        public double collision_particle_random_xz_speed = 0.012D;
        public double collision_particle_upward_speed = 0.002D;
        public double collision_particle_random_upward_speed = 0.002D;
    }
}
