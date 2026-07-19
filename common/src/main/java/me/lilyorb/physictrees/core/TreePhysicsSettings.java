package me.lilyorb.physictrees.core;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class TreePhysicsSettings {
    public static boolean breakCutBlock() {
        return true;
    }

    public static boolean allowLowerBlockFelling() {
        return false;
    }

    public static boolean leafDecay() {
        return true;
    }

    public static double leafDecayTicks() {
        return 20;
    }
    public static double leafDecayTicksRandom() {
        return 10;
    }

    public static double partialBreakProgress() {
        return 0.2D;
    }

    public static double collisionBreakProgress() {
        return 0.5D;
    }

    public static int collisionDamageRefreshTicks() {
        return 20 * 120;
    }

    public static int forceTicks() {
        return 2;
    }

    public static double baseUpwardNudge() {
        return 0.2D;
    }

    public static double getAngularMultiplier() {
        return 2.67676767D;
    }

    public static double counterImpulseNoBreakRatio() {
        return 0.17D;
    }

    public static double counterImpulseBreakRatio() {
        return 0.0D;
    }

    public static double impactProbeDistance() {
        return 0.55D;
    }

    public static int collisionParticleCount() {
        return 8;
    }

    public static double collisionParticleOffsetX() {
        return 0.25D;
    }

    public static double collisionParticleOffsetY() {
        return 0.1D;
    }

    public static double collisionParticleOffsetZ() {
        return 0.25D;
    }

    public static double collisionParticleSpeed() {
        return 0.0D;
    }

    public static double collisionParticleYOffset() {
        return -0.3D;
    }

    public static float collisionParticleGravity() {
        return -0.003F;
    }

    public static int collisionParticleLifetime() {
        return 20;
    }

    public static int collisionParticleLifetimeRandom() {
        return 10;
    }

    public static float collisionParticleAlpha() {
        return 0.75F;
    }

    public static float collisionParticleSize() {
        return 3.2F;
    }

    public static double collisionParticleRandomXzSpeed() {
        return 0.012D;
    }

    public static double collisionParticleUpwardSpeed() {
        return 0.002D;
    }

    public static double collisionParticleRandomUpwardSpeed() {
        return 0.002D;
    }
}
