package com.cappleapple.animatedinventory.api.inventory;

import java.util.*;

/** Immutable collection with defensive stack copies made by VisualItem. Treat stacks as read-only. */
public record InventoryVisualSnapshot(long owner, String providerId, long layoutRevision,
                                      boolean virtualized, Map<String, VisualItem> items) {
    public InventoryVisualSnapshot {
        Objects.requireNonNull(providerId);
        items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }
    public static InventoryVisualSnapshot of(long owner, String provider, long revision, boolean virtualized, List<VisualItem> entries) {
        Map<String, VisualItem> map = new LinkedHashMap<>();
        for (VisualItem entry : entries) {
            if (map.put(entry.id(), entry) != null) throw new IllegalArgumentException("Duplicate visual ID: " + entry.id());
        }
        return new InventoryVisualSnapshot(owner, provider, revision, virtualized, map);
    }
}
