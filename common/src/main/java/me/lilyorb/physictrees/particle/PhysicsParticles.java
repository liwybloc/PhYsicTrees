package me.lilyorb.physictrees.particle;

import me.lilyorb.physictrees.core.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class PhysicsParticles {
    public static final CollisionDustParticleType COLLISION_DUST = new CollisionDustParticleType();

    private PhysicsParticles() {
    }

    public static void register() {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "collision_dust");
        if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) {
            Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, COLLISION_DUST);
        }
    }

    public static final class CollisionDustParticleType extends ParticleType<CollisionDustParticleOptions> {
        private CollisionDustParticleType() {
            super(false);
        }

        @Override
        public com.mojang.serialization.@NotNull MapCodec<CollisionDustParticleOptions> codec() {
            return CollisionDustParticleOptions.CODEC;
        }

        @Override
        public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, CollisionDustParticleOptions> streamCodec() {
            return CollisionDustParticleOptions.STREAM_CODEC;
        }
    }
}
