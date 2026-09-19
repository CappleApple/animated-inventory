package com.cappleapple.animatedinventory;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
/** Initializes the same registry component defaults that game resource loading supplies. */
public final class TestBootstrap {
    private static boolean ready;
    public static synchronized void initialize() {
        if (ready) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        try {
            java.lang.reflect.Method factory;
            try { factory = VanillaRegistries.class.getMethod("createLookup"); }
            catch (NoSuchMethodException modern) { factory = VanillaRegistries.class.getMethod("createWorldLookup"); }
            HolderLookup.Provider lookup = (HolderLookup.Provider)factory.invoke(null);
            // Datagen lookup uses vanilla placeholder HolderSets rather than runtime tag sets.
            // Suppress only NeoForge's IDE-only equals/hashCode check during that vanilla setup.
            boolean ide = SharedConstants.IS_RUNNING_IN_IDE;
            try {
                SharedConstants.IS_RUNNING_IN_IDE = false;
                BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(DataComponentInitializers.PendingComponents::apply);
            } finally { SharedConstants.IS_RUNNING_IN_IDE = ide; }
            ready = true;
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot initialize vanilla registry component defaults", error); }
    }
    private TestBootstrap() {}
}
