package com.cappleapple.animatedinventory.client.transaction;

import com.cappleapple.animatedinventory.api.inventory.InventoryVisualSnapshot;

/** Holds a visual baseline across client prediction and separately delivered server inventory packets. */
public final class SynchronizedTransfer {
    public enum Decision { WAIT, READY, REBASE }
    private static final long TIMEOUT = 750_000_000L, SETTLE = 25_000_000L;
    public final InventoryVisualSnapshot before;
    public final String transactionId;
    private final long initialServerRevision, started;
    private long serverRevision, contentsRevision, changedAt;

    public SynchronizedTransfer(InventoryVisualSnapshot before, String transactionId,
                                long serverRevision, long contentsRevision, long now) {
        this.before = before; this.transactionId = transactionId;
        this.initialServerRevision = this.serverRevision = serverRevision;
        this.contentsRevision = contentsRevision; this.started = this.changedAt = now;
    }
    public Decision observe(InventoryVisualSnapshot after, long server, long contents, long now) {
        if (after.owner() != before.owner() || !after.providerId().equals(before.providerId())
                || after.layoutRevision() != before.layoutRevision() || server < initialServerRevision)
            return Decision.REBASE;
        if (server != serverRevision || contents != contentsRevision) {
            changedAt = now; serverRevision = server; contentsRevision = contents;
        }
        // One quiet interval joins the menu packets and BNS's independently flushed delta.
        if ((initialServerRevision < 0 ? contentsChanged(after) : server > initialServerRevision)
                && now - changedAt >= SETTLE) return Decision.READY;
        return now - started >= TIMEOUT ? Decision.REBASE : Decision.WAIT;
    }
    private boolean contentsChanged(InventoryVisualSnapshot after) {
        if (!before.items().keySet().equals(after.items().keySet())) return true;
        return before.items().values().stream().anyMatch(item ->
                !net.minecraft.world.item.ItemStack.matches(item.stack(), after.items().get(item.id()).stack()));
    }
}
