package com.cappleapple.animatedinventory.client;

import com.cappleapple.animatedinventory.api.AnimatedInventoryApi;
import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.animation.*;
import com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility;
import com.cappleapple.animatedinventory.client.render.*;
import com.cappleapple.animatedinventory.client.transaction.TransactionInference;
import com.cappleapple.animatedinventory.client.transaction.SynchronizedTransfer;
import com.cappleapple.animatedinventory.client.transaction.RecipeViewerOrigins;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import com.cappleapple.animatedinventory.client.transaction.CraftingFlow;
import com.cappleapple.animatedinventory.client.compat.sophisticated.SophisticatedCrafting;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.entity.player.Player;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class ClientRuntime implements AnimatedInventoryApi.Backend {
    public static final ClientRuntime INSTANCE = new ClientRuntime();
    public final AnimationManager animations = new AnimationManager();
    public final EmphasisChannels emphasis = new EmphasisChannels();
    public final ScreenTransitions screens = new ScreenTransitions();
    private final TransactionInference inference = new TransactionInference();
    private final List<CraftingFlow.Consumption> crafting = new ArrayList<>();
    private SophisticatedCrafting.Take sophisticatedTake;
    private SynchronizedTransfer pendingTransfer;
    private final RecipeViewerOrigins viewerOrigins = new RecipeViewerOrigins();
    private final VanillaInventoryProvider vanilla = new VanillaInventoryProvider();
    private final List<InventoryViewProvider> providers = new ArrayList<>();
    private final Map<String, Slot> observedNativeSlots = new HashMap<>();
    private Screen activeScreen;
    private InventoryViewProvider provider;
    private InventoryVisualSnapshot snapshot;
    private long owner, revision, txSequence, lastInteraction;
    private String transactionId = "initial";
    private boolean quickMove, failed, enabledLast;
    public double mouseX, mouseY;
    public long comparisons, transactions, snapshots, renderNanos;

    public void initialize() {
        AnimatedInventoryApi.install(this);
    }
    @Override public boolean enabled() { return ClientConfig.ENABLED.get() && !failed; }
    @Override public AutoCloseable register(InventoryViewProvider value) {
        Objects.requireNonNull(value);
        if (providers.stream().anyMatch(p -> p.id().equals(value.id()))) throw new IllegalArgumentException("Duplicate provider: " + value.id());
        providers.addFirst(value);
        if (activeScreen != null) reset(activeScreen);
        return () -> { providers.remove(value); if (activeScreen != null) reset(activeScreen); };
    }
    @Override public long owner(Screen screen) { ensure(screen); return owner; }
    public void init(Screen screen) { reset(screen); }
    public void opening(Screen current, Screen next) {
        viewerOrigins.opening(current, next, provider == vanilla ? snapshot : null);
    }
    public void closing(Screen screen) {
        if (screen == activeScreen) {
            screens.close(); crafting.clear(); sophisticatedTake = null; pendingTransfer = null; animations.clear(); emphasis.clear(); snapshot = null; provider = null; activeScreen = null; owner++;
        }
    }
    public void tick() {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen != activeScreen) reset(screen);
        boolean enabled = enabled();
        if (enabled != enabledLast) { viewerOrigins.clear(); crafting.clear(); sophisticatedTake = null; pendingTransfer = null; animations.clear(); emphasis.clear(); snapshot = null; screens.discard(); enabledLast = enabled; }
        if (enabled && provider != null) poll(false);
        else if (!enabled) animations.clear();
        emphasis.prune(System.nanoTime());
        if (Minecraft.getInstance().level == null) { screens.discard(); HotbarAnimation.reset(); viewerOrigins.clear(); }
        else if (!RecipeViewerOrigins.viewer(screen) && !(screen instanceof AbstractContainerScreen<?>)) viewerOrigins.clear();
    }
    public void ensure(Screen screen) { if (screen != activeScreen) reset(screen); }
    private void reset(Screen screen) {
        animations.clear(); emphasis.clear(); screens.discardLive();
        crafting.clear(); sophisticatedTake = null; pendingTransfer = null; activeScreen = screen; owner++; snapshot = null; provider = null; failed = false; observedNativeSlots.clear();
        quickMove = false; lastInteraction = 0; revision = 0;
        if (screen == null) return;
        try {
            provider = providers.stream().filter(p -> p.supports(screen)).findFirst().orElse(vanilla.supports(screen) ? vanilla : null);
            if (provider != null && ClientConfig.ENABLED.get()) {
                readMouse(); snapshot = capture(); revision = provider.revision(screen); screens.open();
                pendingTransfer = provider == vanilla ? viewerOrigins.restore(screen, snapshot, revision, "recipe-" + (++txSequence)) : null;
                if (provider != vanilla) viewerOrigins.clear();
                if (pendingTransfer != null) snapshot = pendingTransfer.before;
            }
        } catch (RuntimeException | LinkageError error) { fail(error); }
    }
    private void readMouse() {
        var mc = Minecraft.getInstance();
        mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getScreenWidth());
        mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / Math.max(1, mc.getWindow().getScreenHeight());
    }
    private InventoryVisualSnapshot capture() {
        snapshots++;
        InventoryVisualSnapshot captured = provider.capture(activeScreen, owner, mouseX, mouseY);
        if (captured.owner() != owner || !captured.providerId().equals(provider.id()) || captured.items().size() > 4096)
            throw new IllegalArgumentException("Provider returned invalid ownership, identity, or excessive view size");
        return captured;
    }
    public void beforeInteraction(AbstractContainerScreen<?> screen, Slot slot, ContainerInput type) {
        ensure(screen);
        if (!enabled() || provider == null) return;
        readMouse(); poll(false);
        if (pendingTransfer != null && type != ContainerInput.QUICK_MOVE) {
            pendingTransfer = null; snapshot = null; poll(true);
        }
        if (pendingTransfer == null) { crafting.clear(); sophisticatedTake = null; }
        // Cursor starts at the current mouse position, not the position recorded at the last inventory mutation.
        try {
            if (pendingTransfer == null) {
                snapshot = capture(); sophisticatedTake = SophisticatedCrafting.prepare(screen, slot, type, snapshot);
            }
        } catch (RuntimeException | LinkageError error) { fail(error); return; }
        lastInteraction = System.nanoTime(); transactionId = "input-" + (++txSequence);
        quickMove = type == ContainerInput.QUICK_MOVE;
        long serverRevision = BundledCompatibility.serverRevision();
        if (pendingTransfer == null && provider == vanilla && quickMove && slot != null && slot.hasItem()
                && serverRevision >= 0 && !(screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen)) {
            pendingTransfer = new SynchronizedTransfer(snapshot, transactionId, serverRevision, revision, lastInteraction);
            animations.clear();
        }
        if (slot != null) emphasis.click(VanillaInventoryProvider.id(slot), lastInteraction);
    }
    public void remoteTransfer(boolean recipe) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || !enabled()) return;
        try {
            if (recipe && RecipeViewerOrigins.viewer(mc.gui.screen())) { viewerOrigins.request(mouseX, mouseY); return; }
            if (mc.gui.screen() != activeScreen || provider != vanilla || snapshot == null
                    || !recipe && BundledCompatibility.serverRevision() < 0 || pendingTransfer != null) return;
            readMouse(); poll(false);
            snapshot = capture();
            crafting.clear(); sophisticatedTake = null; animations.clear();
            quickMove = false; lastInteraction = System.nanoTime(); transactionId = "remote-" + (++txSequence);
            pendingTransfer = new SynchronizedTransfer(snapshot, transactionId, BundledCompatibility.serverRevision(),
                    provider.revision(activeScreen), lastInteraction);
        } catch (RuntimeException | LinkageError error) { fail(error); }
    }
    public void craftedIngredient(Player player, Slot result, CraftingContainer grid, int index, ItemStack removed) {
        if (!enabled() || provider != vanilla || snapshot == null || removed.isEmpty() || crafting.size() >= 1024
                || player != Minecraft.getInstance().player || !(activeScreen instanceof AbstractContainerScreen<?> screen)
                || !VanillaInventoryProvider.slots(screen).contains(result) || grid.getWidth() != grid.getHeight()
                || grid.getWidth() < 2 || grid.getWidth() > 3) return;
        VisualItem product = snapshot.items().get(VanillaInventoryProvider.id(result));
        if (product == null || product.stack().isEmpty()) return;
        VanillaInventoryProvider.slots(screen).stream().filter(slot -> slot.container == grid && slot.getContainerSlot() == index)
                .findFirst().ifPresent(slot -> crafting.add(new CraftingFlow.Consumption(
                        VanillaInventoryProvider.id(slot), product.id(), removed, product.stack())));
    }
    /** Called once per menu after its first complete server content update has been applied. */
    public void initialContents(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || !enabled() || provider != vanilla || mc.gui.screen() != activeScreen
                || !(activeScreen instanceof AbstractContainerScreen<?> screen) || screen.getMenu() != menu
                || pendingTransfer != null || lastInteraction != 0) return;
        // Keep the screen opening effect, but never infer item arrivals from the empty opening menu.
        // A transfer already requested by the player must retain its original comparison snapshot.
        invalidate(screen, false);
    }
    public void afterInteraction() { if (enabled() && provider != null) poll(true); }
    public void clickPre(Screen screen, double x, double y) {
        if (screen == activeScreen) {
            mouseX = x; mouseY = y;
            if (snapshot != null && enabled()) snapshot.items().values().stream().filter(i -> i.mayAnimate() && i.bounds().contains(mouseX, mouseY))
                    .forEach(i -> emphasis.click(i.id(), System.nanoTime()));
        }
    }
    private void poll(boolean force) {
        try {
            long nextRevision = provider.revision(activeScreen);
            if (!force && pendingTransfer == null && snapshot != null && nextRevision == revision && crafting.isEmpty() && sophisticatedTake == null) return;
            InventoryVisualSnapshot after = capture();
            String confirmedId = null;
            if (pendingTransfer != null) {
                var decision = pendingTransfer.observe(after, BundledCompatibility.serverRevision(), nextRevision, System.nanoTime());
                if (decision == SynchronizedTransfer.Decision.WAIT) return;
                snapshot = pendingTransfer.before;
                confirmedId = pendingTransfer.transactionId;
                pendingTransfer = null;
                if (decision == SynchronizedTransfer.Decision.REBASE) {
                    snapshot = after; revision = nextRevision; crafting.clear(); sophisticatedTake = null;
                    animations.clear(); return;
                }
            }
            // Reused page cells have new contents without an item transfer. Release old visual claims immediately,
            // including when an acknowledged page arrives on a tick before the next render.
            if (snapshot != null && (snapshot.virtualized() || after.virtualized())
                    && snapshot.layoutRevision() != after.layoutRevision()) {
                crafting.clear(); sophisticatedTake = null; pendingTransfer = null; animations.clear(); emphasis.clear(); screens.discardLive();
            }
            if (snapshot != null) {
                long now = System.nanoTime();
                boolean related = confirmedId != null || now - lastInteraction <= ClientConfig.CORRELATION_MS.get() * 1_000_000L;
                String id = confirmedId != null ? confirmedId : related ? transactionId : "update-" + (++txSequence);
                List<String> suppliedIds = after.items().values().stream().map(VisualItem::transactionId).filter(Objects::nonNull).distinct().limit(2).toList();
                if (suppliedIds.size() == 1) id = suppliedIds.getFirst();
                comparisons++;
                if (sophisticatedTake != null) crafting.addAll(sophisticatedTake.observe(after));
                InventoryVisualTransaction transaction = CraftingFlow.compare(inference, snapshot, after, crafting, id, related && quickMove, ClientConfig.MAX.get());
                crafting.clear(); sophisticatedTake = null; pendingTransfer = null;
                InventoryVisualSnapshot before = snapshot;
                snapshot = after;
                enqueue(transaction, before);
            } else snapshot = after;
            revision = nextRevision;
        } catch (RuntimeException | LinkageError error) { fail(error); }
    }
    public void renderPre(Screen screen, int x, int y) {
        if (screen != Minecraft.getInstance().gui.screen()) return;
        ensure(screen); mouseX = x; mouseY = y;
        if (!enabled()) { animations.clear(); return; }
        // Slot coordinates are cheap to validate every frame; snapshots remain event/change driven.
        if (provider == vanilla && snapshot != null && screen instanceof AbstractContainerScreen<?> container
                && VanillaInventoryProvider.layout(container) != snapshot.layoutRevision()) {
            animations.clear(); emphasis.clear(); screens.discardLive(); poll(true);
        }
        animations.update(System.nanoTime(), Bounds.item(mouseX - 8, mouseY - 8), a -> {
            if (a.transition.type() == TransitionType.MERGE) emphasis.merge(a.transition.destinationId(), System.nanoTime());
        });
    }
    public void foreground(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen) {
        if (screen == activeScreen && enabled()) AnimationRenderer.render(graphics, this);
    }
    public void hud(GuiGraphicsExtractor graphics) { screens.renderExit(graphics); }
    @Override public List<Long> submit(Screen screen, InventoryVisualTransaction transaction) {
        ensure(screen);
        if (!enabled() || transaction.owner() != owner || provider == null) return List.of();
        try {
            pendingTransfer = null; crafting.clear(); sophisticatedTake = null;
            snapshot = capture(); revision = provider.revision(screen);
            return enqueue(transaction);
        } catch (RuntimeException | LinkageError error) { fail(error); return List.of(); }
    }
    private List<Long> enqueue(InventoryVisualTransaction transaction) { return enqueue(transaction, snapshot); }
    private List<Long> enqueue(InventoryVisualTransaction transaction, InventoryVisualSnapshot before) {
        if (transaction.transitions().isEmpty() || !provider.controlsNormalRendering()) return List.of();
        transactions++;
        if (ClientConfig.LOG.get()) LogUtils.getLogger().debug("Animated Inventory transaction {}: {} transitions, owner {}", transaction.id(), transaction.transitions().size(), owner);
        long now = System.nanoTime();
        List<Long> handles = new ArrayList<>();
        Map<ItemTransition, Bounds> starts = new IdentityHashMap<>();
        Set<String> touched = new HashSet<>();
        boolean interrupted = false;
        for (ItemTransition t : transaction.transitions()) {
            starts.put(t, animations.current(t.sourceId(), t.source(), now));
            interrupted |= animations.destination(t.sourceId(), now) != null || animations.destination(t.destinationId(), now) != null;
            if (t.sourceId() != null) touched.add(t.sourceId()); if (t.destinationId() != null) touched.add(t.destinationId());
        }
        animations.cancelTouching(touched);
        if (interrupted && ClientConfig.INTERRUPTION.get() == ClientConfig.Interruption.CANCEL) return handles;
        boolean inline = InventoryParticlesCompatibility.inlineOnly();
        Map<String, Long> destinationCounts = new HashMap<>();
        for (ItemTransition t : transaction.transitions()) if (t.destinationId() != null && t.type() != TransitionType.CRAFT) destinationCounts.merge(t.destinationId(), 1L, Long::sum);
        for (ItemTransition t : transaction.transitions()) {
            if (t.type() == TransitionType.COUNT_CHANGE) { emphasis.merge(t.destinationId(), now); continue; }
            if (!allows(t, transaction.quickMove())) continue;
            VisualItem target = snapshot.items().get(t.destinationId());
            boolean craft = t.type() == TransitionType.CRAFT;
            VisualItem origin = before.items().get(t.sourceId());
            boolean retrieve = t.type() == TransitionType.RETRIEVE && origin != null && origin.offscreenSource();
            boolean offscreen = (t.type() == TransitionType.STOW || craft) && target != null && target.offscreenDestination();
            if (provider == vanilla && (!retrieve && !nativeEndpoint(t.sourceId()) || !offscreen && !nativeEndpoint(t.destinationId()))) continue;
            if (offscreen && t.sourceId() == null) continue;
            if (t.destinationId() != null && (target == null || !target.visible() && !offscreen || !target.mayAnimate()
                    || !craft && (!ItemStack.isSameItemSameComponents(target.stack(), t.stack()) || t.stack().getCount() > target.stack().getCount()))) continue;
            boolean nativeTransform = inline && !offscreen && !retrieve && !craft && target != null
                    && target.stack().getCount() == t.stack().getCount()
                    && destinationCounts.getOrDefault(t.destinationId(), 0L) == 1
                    && t.type() != TransitionType.APPEAR;
            // Known-source split/merge copies bypass GuiGraphicsExtractor item hooks, so every donor can travel
            // without adding duplicate Inventory Particles effects.
            if (inline && !nativeTransform && !offscreen && !craft && (t.sourceId() == null || t.destinationId() == null)) {
                emphasis.merge(t.destinationId(), now); continue;
            }
            AnimationOptions options = ClientConfig.options(t, transaction.quickMove());
            Bounds start = starts.get(t);
            long duration = ClientConfig.nanos(options.durationMs());
            if (interrupted && ClientConfig.INTERRUPTION.get() == ClientConfig.Interruption.FINISH_FAST) {
                start = t.source(); duration = Math.min(duration, 45_000_000);
            }
            if (ClientConfig.REDUCE_MOTION.get()) {
                if (offscreen || retrieve || craft) continue;
                if (inline) { emphasis.merge(t.destinationId(), now); continue; }
                start = t.destination(); duration = Math.min(duration, 60_000_000);
                options = new AnimationOptions(options.durationMs(), Easing.LINEAR, MovementStyle.LINEAR, 0,
                        1, 1, t.type() == TransitionType.DISAPPEAR ? 1 : 0, t.type() == TransitionType.DISAPPEAR ? 0 : 1,
                        0, 0, options.zOrder(), false);
            }
            long handle = animations.add(owner, transaction.id(), t, options, now, duration, ClientConfig.MAX.get(), nativeTransform, start);
            if (handle >= 0) handles.add(handle);
        }
        return handles;
    }
    private boolean allows(ItemTransition t, boolean quick) {
        if (t.type() == TransitionType.CRAFT) return ClientConfig.CRAFT.get() && ClientConfig.MOVEMENT.get() && (!quick || ClientConfig.QUICK.get());
        if (quick && !ClientConfig.QUICK.get()) return false;
        if ("cursor".equals(t.destinationId()) && !ClientConfig.PICKUP.get()) return false;
        if ("cursor".equals(t.sourceId()) && !ClientConfig.PLACEMENT.get()) return false;
        if (t.type() == TransitionType.LAYOUT_REFLOW) return ClientConfig.REFLOW.get();
        if (t.type() == TransitionType.APPEAR) return ClientConfig.APPEAR.get() != ClientConfig.ItemEffect.NONE;
        if (t.type() == TransitionType.DISAPPEAR) return ClientConfig.DISAPPEAR.get() != ClientConfig.ItemEffect.NONE;
        return ClientConfig.MOVEMENT.get();
    }
    @Override public Optional<Bounds> bounds(Screen screen, String id, boolean animated) {
        if (screen != activeScreen || snapshot == null) return Optional.empty();
        VisualItem item = snapshot.items().get(id);
        if (item == null || !item.visible()) return Optional.empty();
        return Optional.of(animated && enabled() ? animations.current(id, item.bounds(), System.nanoTime()) : item.bounds());
    }
    @Override public Optional<Bounds> normalBounds(Screen screen, String id) {
        ItemAnimation animation = enabled() && screen == activeScreen ? animations.destination(id, System.nanoTime()) : null;
        return bounds(screen, id, animation != null && animation.inline);
    }
    @Override public ItemStack normalStack(Screen screen, String id, ItemStack real) {
        if (!enabled() || screen != activeScreen) return real;
        VisualItem expected = snapshot == null ? null : snapshot.items().get(id);
        if (pendingTransfer != null && expected != null && expected.mayAnimate()) return expected.stack().copy();
        if (expected == null || !ItemStack.matches(expected.stack(), real)) {
            animations.cancelTouching(Set.of(id));
            return real;
        }
        return animations.normalStack(id, real, System.nanoTime());
    }
    @Override public void invalidate(Screen screen, boolean reflow) {
        if (screen != activeScreen || provider == null) return;
        crafting.clear(); sophisticatedTake = null; pendingTransfer = null; animations.clear(); emphasis.clear(); if (!reflow) snapshot = null; poll(true);
    }
    @Override public void render(Screen screen, GuiGraphicsExtractor graphics) {
        if (enabled() && screen == activeScreen && provider != null) AnimationRenderer.render(graphics, this);
    }
    @Override public void cancel(long handle) { animations.cancel(handle); }
    public void observeNativeSlot(Screen screen, Slot slot) {
        if (screen == activeScreen && screen instanceof AbstractContainerScreen<?> container
                && VanillaInventoryProvider.ownsSlot(container, slot)) {
            observedNativeSlots.put(VanillaInventoryProvider.id(slot), slot);
        }
    }
    private boolean nativeEndpoint(String id) {
        if (id == null || id.equals("cursor")) return true;
        Slot slot = observedNativeSlots.get(id);
        return slot != null && activeScreen instanceof AbstractContainerScreen<?> screen
                && VanillaInventoryProvider.ownsSlot(screen, slot) && id.equals(VanillaInventoryProvider.id(slot));
    }
    /** Cosmetic read used before native renderers choose their empty-slot or item branch. */
    public ItemStack pendingStack(Screen screen, Slot slot, ItemStack real) {
        if (pendingTransfer == null || !canAnimateSlot(screen, slot)) return real;
        return snapshot.items().get(VanillaInventoryProvider.id(slot)).stack().copy();
    }
    public boolean canAnimateSlot(Screen screen, Slot slot) {
        if (!enabled() || screen != activeScreen || snapshot == null
                || !(screen instanceof AbstractContainerScreen<?> container)
                || !VanillaInventoryProvider.ownsSlot(container, slot)) return false;
        VisualItem item = snapshot.items().get(VanillaInventoryProvider.id(slot));
        return item != null && item.mayAnimate() && item.visible();
    }
    public InventoryVisualSnapshot snapshot() { return snapshot; }
    public void fail(Throwable error) {
        viewerOrigins.clear();
        failed = true; crafting.clear(); sophisticatedTake = null; pendingTransfer = null; animations.clear(); emphasis.clear(); screens.discard();
        LogUtils.getLogger().warn("Animated Inventory disabled visuals for this screen; normal inventory rendering remains active", error);
    }
}
