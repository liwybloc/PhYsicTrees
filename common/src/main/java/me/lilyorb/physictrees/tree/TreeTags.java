package me.lilyorb.physictrees.tree;

import lombok.experimental.UtilityClass;
import me.lilyorb.physictrees.core.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

@UtilityClass
public final class TreeTags {
    public static final TagKey<Block> LOGS = blockTag("logs");
    public static final TagKey<Block> LEAVES = blockTag("leaves");
    public static final TagKey<Block> TREE = blockTag("tree");
    public static final TagKey<Block> ROOTS = blockTag("roots");
    public static final TagKey<Block> CAN_BE_ROOTS = blockTag("can_be_roots");
    public static final TagKey<Block> ATTACHED_BLOCKS = blockTag("attached_blocks");

    private static TagKey<Block> blockTag(final String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
    }
}
