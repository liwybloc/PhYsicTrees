package me.lilyorb.physictrees.neoforge;

import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.core.Constants;
import me.lilyorb.physictrees.particle.PhysicsParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

@UtilityClass
public final class PhYsicTreesNeoForgeParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Constants.MOD_ID);

    static {
        PARTICLES.register("collision_dust", () -> PhysicsParticles.COLLISION_DUST);
    }

    public static void register(final IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }
}
