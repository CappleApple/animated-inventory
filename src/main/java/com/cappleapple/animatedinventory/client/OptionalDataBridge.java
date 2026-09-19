package com.cappleapple.animatedinventory.client;
import java.lang.reflect.Method;
import java.util.function.Supplier;
/** Optional public attachment access without linking NeoForge classes on Fabric. */
public final class OptionalDataBridge {
    private static Method hasData, getData;
    private static boolean resolved;
    private static void resolve(Object player) {
        if (resolved) return;
        resolved = true;
        for (Method method : player.getClass().getMethods()) {
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != Supplier.class) continue;
            if (method.getName().equals("hasData")) hasData = method;
            if (method.getName().equals("getData")) getData = method;
        }
    }
    public static boolean hasData(Object player, Supplier<?> attachment) {
        resolve(player);
        try { return hasData != null && Boolean.TRUE.equals(hasData.invoke(player, attachment)); }
        catch (ReflectiveOperationException exception) { return false; }
    }
    public static Object getData(Object player, Supplier<?> attachment) {
        resolve(player);
        try { return getData == null ? null : getData.invoke(player, attachment); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot read optional inventory attachment", exception); }
    }
    private OptionalDataBridge() {}
}
