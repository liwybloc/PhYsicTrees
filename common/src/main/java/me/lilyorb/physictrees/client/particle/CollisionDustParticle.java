package me.lilyorb.physictrees.client.particle;

import me.lilyorb.physictrees.particle.CollisionDustParticleOptions;
import me.lilyorb.physictrees.physics.TreePhysicsSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public final class CollisionDustParticle extends TextureSheetParticle {
    private static final int DIRT_DUST_COLOR = 0x8A6A45;

    private final BlockPos pos;
    private final SpriteSet sprites;

    private CollisionDustParticle(final ClientLevel level, final double x, final double y, final double z, final double xSpeed, final double ySpeed, final double zSpeed, final CollisionDustParticleOptions options, final SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pos = options.pos();
        this.sprites = sprites;
        this.pickSprite(sprites);
        this.gravity = TreePhysicsSettings.collisionParticleGravity();
        this.lifetime = Math.max(1, TreePhysicsSettings.collisionParticleLifetime())
                + this.random.nextInt(Math.max(1, TreePhysicsSettings.collisionParticleLifetimeRandom()));
        this.setAlpha(TreePhysicsSettings.collisionParticleAlpha());
        this.rCol = 0.6F;
        this.gCol = 0.6F;
        this.bCol = 0.6F;
        this.applyBlockColor(level, options.state());
        this.quadSize *= TreePhysicsSettings.collisionParticleSize();
        this.xd = (this.random.nextDouble() * 2.0D - 1.0D) * TreePhysicsSettings.collisionParticleRandomXzSpeed();
        this.yd = TreePhysicsSettings.collisionParticleUpwardSpeed()
                + this.random.nextDouble() * TreePhysicsSettings.collisionParticleRandomUpwardSpeed();
        this.zd = (this.random.nextDouble() * 2.0D - 1.0D) * TreePhysicsSettings.collisionParticleRandomXzSpeed();
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(final float partialTick) {
        final int color = super.getLightColor(partialTick);
        return color == 0 && this.level.hasChunkAt(this.pos) ? LevelRenderer.getLightColor(this.level, this.pos) : color;
    }

    private void applyBlockColor(final ClientLevel level, final BlockState state) {
        final BlockState colorState = state.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT.defaultBlockState() : state;
        final int color = this.dustColorFor(level, colorState);
        if (color == -1) {
            return;
        }

        this.rCol *= (float) ((color >> 16) & 0xFF) / 255.0F;
        this.gCol *= (float) ((color >> 8) & 0xFF) / 255.0F;
        this.bCol *= (float) (color & 0xFF) / 255.0F;
    }

    private int dustColorFor(final ClientLevel level, final BlockState state) {
        if (state.is(Blocks.DIRT)) {
            return DIRT_DUST_COLOR;
        }

        final int blockColor = Minecraft.getInstance().getBlockColors().getColor(state, level, this.pos, 0);
        if (blockColor != -1) {
            return blockColor;
        }

        final int mapColor = state.getMapColor(level, this.pos).col;
        return mapColor == MapColor.NONE.col ? -1 : mapColor;
    }

    public static final class Provider implements ParticleProvider<CollisionDustParticleOptions> {
        private final SpriteSet sprites;

        public Provider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public TextureSheetParticle createParticle(final CollisionDustParticleOptions options, final ClientLevel level, final double x, final double y, final double z, final double xSpeed, final double ySpeed, final double zSpeed) {
            return new CollisionDustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, this.sprites);
        }
    }
}
