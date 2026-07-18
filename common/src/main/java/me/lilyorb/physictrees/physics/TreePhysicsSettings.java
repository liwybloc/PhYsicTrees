package me.lilyorb.physictrees.physics;

public final class TreePhysicsSettings {
    private TreePhysicsSettings() {
    }

    public static boolean breakCutBlock() {
        return true;
    }

    public static boolean allowLowerBlockFelling() {
        return false;
    }

    public static double partialBreakProgress() {
        return 0.2D;
    }

    public static double collisionBreakProgress() {
        return 0.5D;
    }

    public static int collisionDamageRefreshTicks() {
        return 20 * 30;
    }

    public static int forceTicks() {
        return 10;
    }

    public static double baseAngularNudge() {
        return 1.3D;
    }

    public static double baseUpwardNudge() {
        return 0.2D;
    }

    public static double logForceAmplitude() {
        return 1.2D;
    }

    public static double leafForceAmplitude() {
        return 0.4D;
    }

    public static double counterImpulseNoBreakRatio() {
        return 0.18D;
    }

    public static double counterImpulseBreakRatio() {
        return -0.08D;
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
        return 40;
    }

    public static int collisionParticleLifetimeRandom() {
        return 20;
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
        return 0.006D;
    }
}
