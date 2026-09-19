package com.cappleapple.animatedinventory.client;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
/** The loader installs this boundary before initializing optional adapters. */
public final class Platform {
    private static Function<ItemStack, Font> countFont = stack -> null;
    public static void installCountFont(Function<ItemStack, Font> lookup) { countFont = java.util.Objects.requireNonNull(lookup); }
    public static Font countFont(ItemStack stack) {
        Font font = countFont.apply(stack);
        return font == null ? Minecraft.getInstance().font : font;
    }
    private static Predicate<String> loaded = id -> false;
    public static void install(Predicate<String> lookup) { loaded = lookup; }
    public static boolean isLoaded(String id) { return loaded.test(id); }
    public static boolean hasData(Object player, Supplier<?> type) { return OptionalDataBridge.hasData(player, type); }
    public static Object getData(Object player, Supplier<?> type) { return OptionalDataBridge.getData(player, type); }
    private Platform() {}
}
