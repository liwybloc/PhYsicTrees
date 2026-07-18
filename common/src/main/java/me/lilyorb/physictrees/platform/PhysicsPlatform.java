package me.lilyorb.physictrees.platform;

import java.lang.reflect.InvocationTargetException;

public final class PhysicsPlatform {
    private PhysicsPlatform() {
    }

    public static boolean isModLoaded(final String modId) {
        return isNeoForgeModLoaded(modId) || isFabricModLoaded(modId);
    }

    private static boolean isNeoForgeModLoaded(final String modId) {
        try {
            final Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            final Object modList = modListClass.getMethod("get").invoke(null);
            final Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
            return Boolean.TRUE.equals(loaded);
        } catch (final ClassNotFoundException ignored) {
            return false;
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalStateException("Could not query NeoForge mod list.", exception);
        }
    }

    private static boolean isFabricModLoaded(final String modId) {
        try {
            final Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            final Object loader = loaderClass.getMethod("getInstance").invoke(null);
            final Object loaded = loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
            return Boolean.TRUE.equals(loaded);
        } catch (final ClassNotFoundException ignored) {
            return false;
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalStateException("Could not query Fabric mod list.", exception);
        }
    }
}
