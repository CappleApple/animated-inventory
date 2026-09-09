package com.cappleapple.animatedinventory.api.inventory;

import java.util.List;

public record InventoryVisualTransaction(long owner, String id, boolean quickMove, List<ItemTransition> transitions) {
    public InventoryVisualTransaction { transitions = List.copyOf(transitions); }
}
