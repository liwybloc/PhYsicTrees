package me.lilyorb.physictrees.core;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;

@UtilityClass
public final class Constants {
    public static final String MOD_ID = "physictrees";
    public static final int ICON_SIZE = 8;
    public static final int ICON_COLOR = 0xE0D18B2A;
    public static final int ICON_OUTLINE = 0xE02B1A08;

    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
