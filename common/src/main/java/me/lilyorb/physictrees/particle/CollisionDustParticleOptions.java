package me.lilyorb.physictrees.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public record CollisionDustParticleOptions(BlockState state, BlockPos pos) implements ParticleOptions {
    public static final MapCodec<CollisionDustParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("state").forGetter(CollisionDustParticleOptions::state),
            BlockPos.CODEC.fieldOf("pos").forGetter(CollisionDustParticleOptions::pos)
    ).apply(instance, CollisionDustParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CollisionDustParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(Block::stateById, Block::getId),
            CollisionDustParticleOptions::state,
            BlockPos.STREAM_CODEC,
            CollisionDustParticleOptions::pos,
            CollisionDustParticleOptions::new
    );

    @Override
    public @NotNull ParticleType<?> getType() {
        return PhysicsParticles.COLLISION_DUST;
    }
}
