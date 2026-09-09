package com.cappleapple.animatedinventory.client.animation;

import com.cappleapple.animatedinventory.client.ClientConfig;
import java.util.*;

public final class EmphasisChannels {
    private static final class Channel {
        final Tween hover = new Tween(1);
        long click = Long.MIN_VALUE / 2, merge = Long.MIN_VALUE / 2, last;
    }
    private final Map<String, Channel> channels = new HashMap<>();
    public void click(String id, long now) { if (id != null) channels.computeIfAbsent(id, key -> new Channel()).click = now; }
    public void merge(String id, long now) { if (id != null) channels.computeIfAbsent(id, key -> new Channel()).merge = now; }
    public double scale(String id, boolean hover, long now) {
        if (!ClientConfig.ENABLED.get()) return 1;
        Channel c = channels.get(id);
        if (c == null && !hover) return 1;
        if (c == null) { c = new Channel(); channels.put(id, c); }
        c.last = now;
        boolean reduced = ClientConfig.REDUCE_MOTION.get();
        double hoverTarget = ClientConfig.HOVER.get() && hover ? (reduced ? 1.015 : ClientConfig.HOVER_SCALE.get()) : 1;
        c.hover.target(hoverTarget, now, ClientConfig.nanos(ClientConfig.HOVER_MS.get()), ClientConfig.HOVER_EASING.get());
        double click = ClientConfig.CLICK.get() ? pulse(now - c.click, ClientConfig.nanos(ClientConfig.CLICK_MS.get())) : 0;
        double merge = ClientConfig.MERGE.get() ? pulse(now - c.merge, ClientConfig.nanos(ClientConfig.MERGE_MS.get())) : 0;
        return c.hover.value(now) * (1 + (ClientConfig.CLICK_SCALE.get() - 1) * click * (reduced ? .2 : 1))
                * (1 + (ClientConfig.MERGE_SCALE.get() - 1) * merge * (reduced ? .2 : 1));
    }
    public void prune(long now) { channels.values().removeIf(c -> now - c.last > 2_000_000_000L && now - c.click > 2_000_000_000L && now - c.merge > 2_000_000_000L); }
    private static double pulse(long elapsed, long duration) { return elapsed >= duration || elapsed < 0 ? 0 : Math.sin(Math.PI * elapsed / duration); }
    public void clear() { channels.clear(); }
}
