package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.api.AnimatedInventoryApi;
import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.compat.sophisticated.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.lang.reflect.*;
import java.util.*;
import static com.cappleapple.animatedinventory.validation.ClientValidation.require;

/** Real optional-mod screens and client menu operations in the isolated validation world. */
final class SophisticatedValidation {
    private static final String[] CASES = {"backpack", "diamond_backpack", "chest", "netherite_barrel"};
    private static int scenario, step, wait;
    private static long next;
    private static AbstractContainerScreen<?> screen;
    private static Slot source, target, playerSlot;
    private static Bounds initial;
    private static Object wrapper;
    private static boolean inline;
    private static List<Slot> recipe;
    private static Slot result;

    static void tick(Minecraft mc) throws Exception {
        if (++wait < 30 || System.nanoTime() < next) return;
        next = System.nanoTime() + 220_000_000L;
        var runtime = ClientRuntime.INSTANCE;
        if (step == 0) {
            ClientConfig.MOVE_MS.set(900); ClientConfig.PICKUP_MS.set(900); ClientConfig.PLACE_MS.set(900);
            ClientConfig.QUICK_MS.set(900); ClientConfig.CRAFT_MS.set(900); ClientConfig.OPEN.set(ClientConfig.ScreenEffect.NONE);
            ClientConfig.CLOSE.set(ClientConfig.ScreenEffect.NONE); ClientConfig.REDUCE_MOTION.set(false);
            inline = net.neoforged.fml.ModList.get().isLoaded("inventory_particles");
            open(mc, CASES[scenario]);
            require(screen instanceof SophisticatedView, "actual Sophisticated screen has adapter: " + CASES[scenario]);
            require(screen.getMenu() instanceof SophisticatedMenu, "actual menu exposes additional upgrade slots: " + CASES[scenario]);
            source = screen.getMenu().getSlot(0); target = screen.getMenu().getSlot(2);
            playerSlot = screen.getMenu().slots.stream().filter(s -> s.container == mc.player.getInventory() && s.getContainerSlot() == 9).findFirst().orElseThrow();
            source.set(new ItemStack(Items.DIAMOND, 32));
            target.set(ItemStack.EMPTY); screen.getMenu().setCarried(ItemStack.EMPTY);
            runtime.invalidate(screen, false);
        } else if (step == 1) {
            require(runtime.snapshot().items().size() == VanillaInventoryProvider.slots(screen).size() + 1, "upgrade slots included in snapshot: " + CASES[scenario]);
            click(mc, source, 0, ClickType.PICKUP);
            require(screen.getMenu().getCarried().getCount() == 32, "pickup updates real cursor immediately: " + CASES[scenario]);
            motion("cursor", 32);
            capture("pickup");
        } else if (step == 2) {
            motion("cursor", 32); capture("pickup-flight");
        } else if (step == 3) {
            click(mc, target, 0, ClickType.PICKUP);
            require(target.getItem().getCount() == 32, "placement updates real storage immediately: " + CASES[scenario]);
            motion(VanillaInventoryProvider.id(target), 32); capture("placement");
        } else if (step == 4) {
            motion(VanillaInventoryProvider.id(target), 32); capture("placement-flight");
        } else if (step == 5) {
            click(mc, target, 0, ClickType.QUICK_MOVE);
            require(target.getItem().isEmpty(), "storage quick move empties source: " + CASES[scenario]);
            require(runtime.animations.active().stream().anyMatch(a -> VanillaInventoryProvider.id(target).equals(a.transition.sourceId())), "storage-to-player quick move animates: " + CASES[scenario]);
            capture("quick-move");
        } else if (step == 6) {
            source.set(new ItemStack(Items.IRON_INGOT, 48)); playerSlot.set(new ItemStack(Items.IRON_INGOT, 32));
            runtime.invalidate(screen, false);
            click(mc, playerSlot, 0, ClickType.QUICK_MOVE);
            require(source.getItem().getCount() == 64 && screen.getMenu().getSlot(1).getItem().getCount() == 16, "player-to-storage distributes 16 and 16: " + CASES[scenario]);
            if (!inline) {
                motion(VanillaInventoryProvider.id(source), 16);
                motion(VanillaInventoryProvider.id(screen.getMenu().getSlot(1)), 16);
            }
            capture("split-merge");
        } else if (step == 7) {
            runtime.animations.clear();
            // The real stack upgrade raises capacity and enables the original abbreviated count renderer.
            Object upgrades = wrapper.getClass().getMethod("getUpgradeHandler").invoke(wrapper);
            ItemStack upgrade = item("sophisticated" + (scenario < 2 ? "backpacks" : "storage") + ":stack_upgrade_tier_4");
            upgrades.getClass().getMethod("setStackInSlot", int.class, ItemStack.class).invoke(upgrades, 0, upgrade);
            source.set(new ItemStack(Items.EMERALD, 1024));
            target.set(ItemStack.EMPTY); screen.getMenu().getSlot(1).set(ItemStack.EMPTY);
            runtime.invalidate(screen, false);
        } else if (step == 8) {
            require(source.getMaxStackSize(source.getItem()) >= 1024, "actual stack upgrade allows oversized stacks: " + CASES[scenario] + " capacity=" + source.getMaxStackSize(source.getItem()));
            target.set(source.getItem().copy()); source.set(ItemStack.EMPTY);
            runtime.afterInteraction();
            motion(VanillaInventoryProvider.id(target), 1024);
            capture("large-stack");
        } else if (step == 9) {
            motion(VanillaInventoryProvider.id(target), 1024); capture("large-stack-flight");
        } else if (step == 10) {
            runtime.animations.clear();
            Method filter = screen.getClass().getMethod("setExternalSearchPhrase", String.class);
            filter.invoke(screen, "emerald");
            runtime.afterInteraction();
            require(runtime.animations.active().isEmpty(), "filter reflow creates no fake item moves: " + CASES[scenario]);
            require(runtime.snapshot().items().get(VanillaInventoryProvider.id(target)).visible(), "matching filter result remains visible: " + CASES[scenario]);
            require(!runtime.snapshot().items().get(VanillaInventoryProvider.id(source)).visible(), "filtered-out slots are hidden: " + CASES[scenario]);
            capture("filter");
        } else if (step == 11) {
            screen.getClass().getMethod("setExternalSearchPhrase", String.class).invoke(screen, "");
            runtime.afterInteraction();
            require(runtime.animations.active().isEmpty(), "clearing filter releases prior geometry: " + CASES[scenario]);
            if (scenario % 2 == 1) {
                Slot last = screen.getMenu().getSlot(((SophisticatedMenu)screen.getMenu()).getNumberOfStorageInventorySlots() - 1);
                require(!runtime.snapshot().items().get(VanillaInventoryProvider.id(last)).visible(), "offscreen row is not an animated endpoint: " + CASES[scenario]);
                last.set(new ItemStack(Items.GOLD_INGOT, 17));
                double x = screen.getGuiLeft() + 20, y = screen.getGuiTop() + 30;
                for (int i = 0; i < 24; i++) screen.mouseScrolled(x, y, 0, -1);
                runtime.afterInteraction();
                require(runtime.snapshot().items().get(VanillaInventoryProvider.id(last)).visible(), "scrolling exposes final storage row: " + CASES[scenario]);
                require(!runtime.snapshot().items().get(VanillaInventoryProvider.id(source)).visible(), "scrolled-out source stays hidden: " + CASES[scenario]);
                require(runtime.animations.active().isEmpty(), "scrolling cancels old ownership: " + CASES[scenario]);
                source = last;
            }
        } else if (step == 12) {
            source.set(new ItemStack(Items.GOLD_INGOT, 17)); screen.getMenu().setCarried(ItemStack.EMPTY);
            runtime.invalidate(screen, false); click(mc, source, 0, ClickType.PICKUP);
            motion("cursor", 17); capture("scrolled-pickup");
        } else if (step == 13) {
            long owner = runtime.owner(screen);
            screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            require(runtime.owner(screen) != owner && runtime.animations.active().isEmpty(), "resize releases ownership: " + CASES[scenario]);
            screen.getMenu().setCarried(ItemStack.EMPTY); runtime.invalidate(screen, false);
        } else if (step == 14) {
            source = screen.getMenu().getSlot(0); source.set(new ItemStack(Items.LAPIS_LAZULI, 18));
            runtime.invalidate(screen, false); ClientConfig.REDUCE_MOTION.set(true);
            click(mc, source, 0, ClickType.PICKUP);
            require(runtime.animations.active().stream().allMatch(a -> a.duration <= 60_000_000L), "reduced motion respects limit: " + CASES[scenario]);
            ClientConfig.REDUCE_MOTION.set(false);
        } else if (step == 15) {
            screen.getMenu().setCarried(ItemStack.EMPTY);
            source.set(new ItemStack(Items.GOLD_INGOT, 17)); runtime.invalidate(screen, false);
            click(mc, source, 0, ClickType.PICKUP);
            mc.setScreen(new GenericMessageScreen(Component.literal("Validation transition")));
            require(runtime.animations.active().isEmpty(), "closing during travel releases ownership: " + CASES[scenario]);
            mc.setScreen(screen);
            require(runtime.snapshot() != null && runtime.animations.active().isEmpty(), "return creates a fresh snapshot: " + CASES[scenario]);
        } else if (step == 16 && scenario % 2 == 0) {
            Object container = installUpgrade(mc, "crafting_upgrade");
            recipe = (List<Slot>)container.getClass().getMethod("getRecipeSlots").invoke(container);
            result = ((List<Slot>)container.getClass().getMethod("getSlots").invoke(container)).stream().filter(s -> s instanceof ResultSlot).findFirst().orElseThrow();
            recipe.forEach(s -> s.set(ItemStack.EMPTY)); recipe.getFirst().set(new ItemStack(Items.OAK_LOG, 3));
            result.set(new ItemStack(Items.OAK_PLANKS, 4)); screen.getMenu().setCarried(ItemStack.EMPTY);
            runtime.invalidate(screen, false);
        } else if (step == 17 && scenario % 2 == 0) {
            require(runtime.snapshot().items().get(VanillaInventoryProvider.id(recipe.getFirst())).visible(), "crafting upgrade inputs are visible: " + CASES[scenario]);
            require(runtime.animations.active().isEmpty(), "upgrade recipe preview alone does not craft: " + CASES[scenario]);
            click(mc, result, 0, ClickType.PICKUP);
            require(screen.getMenu().getCarried().getCount() == 4, "crafting upgrade result is immediately carried: " + CASES[scenario]);
            require(runtime.animations.active().stream().anyMatch(a -> a.transition.type() == TransitionType.CRAFT && a.transition.stack().is(Items.OAK_LOG)), "crafting upgrade ingredient flows into product: " + CASES[scenario]);
            capture("upgrade-crafting");
        } else if (step == 18 && scenario % 2 == 0) {
            require(AnimatedInventoryApi.normalRenderStack(screen, VanillaInventoryProvider.id(recipe.getFirst()), recipe.getFirst().getItem()).getCount() == 3, "upgrade refill grid keeps normal rendering: " + CASES[scenario]);
            capture("upgrade-crafting-flight");
        } else if (step == 19 && scenario % 2 == 0) {
            runtime.animations.clear(); screen.getMenu().setCarried(ItemStack.EMPTY);
            recipe.getFirst().set(new ItemStack(Items.OAK_LOG, 2)); result.set(new ItemStack(Items.OAK_PLANKS, 4));
            runtime.afterInteraction();
            require(runtime.animations.active().stream().noneMatch(a -> a.transition.type() == TransitionType.CRAFT), "server preview refresh does not repeat ingredient flow: " + CASES[scenario]);
            runtime.invalidate(screen, false); click(mc, result, 0, ClickType.QUICK_MOVE);
            require(runtime.animations.active().stream().anyMatch(a -> a.transition.type() == TransitionType.CRAFT), "shift crafting upgrade flows into stored result: " + CASES[scenario]);
            capture("upgrade-shift-crafting");
        } else if (step == 20 && scenario % 2 == 0) {
            Object container = installUpgrade(mc, "pickup_upgrade");
            recipe = (List<Slot>)container.getClass().getMethod("getSlots").invoke(container);
            runtime.invalidate(screen, false);
        } else if (step == 21 && scenario % 2 == 0) {
            Slot ghost = recipe.getFirst();
            require(!runtime.snapshot().items().get(VanillaInventoryProvider.id(ghost)).mayAnimate(), "upgrade ghost filters cannot own inventory items: " + CASES[scenario]);
            ghost.set(new ItemStack(Items.DIAMOND)); runtime.afterInteraction();
            require(runtime.animations.active().isEmpty(), "ghost filter edits create no transfer animation: " + CASES[scenario]);
        } else if (step == 22) {
            screen.getMenu().getClass().getMethod("updateAdditionalSlotInfo", Set.class, Set.class, Map.class, Set.class, Map.class)
                    .invoke(screen.getMenu(), Set.of(1), Set.of(), Map.of(), Set.of(2), Map.of());
            runtime.afterInteraction();
            require(!runtime.snapshot().items().get("slot:1").mayAnimate(), "inaccessible storage cells cannot claim animations: " + CASES[scenario]);
            require(!runtime.snapshot().items().get("slot:2").mayAnimate(), "infinite storage cells retain native rendering: " + CASES[scenario]);
            require(runtime.animations.active().isEmpty(), "slot permission synchronization rebases visuals: " + CASES[scenario]);
            long layout = VanillaInventoryProvider.layout(screen);
            screen.getMenu().getClass().getMethod("updateAdditionalSlotInfo", Set.class, Set.class, Map.class, Set.class, Map.class)
                    .invoke(screen.getMenu(), Set.of(1), Set.of(), Map.of(), Set.of(2), Map.of());
            require(VanillaInventoryProvider.layout(screen) == layout, "identical permission packet preserves geometry: " + CASES[scenario]);
            mc.setScreen(null); mc.player.containerMenu = mc.player.inventoryMenu;
            scenario++; step = -1;
            if (scenario == CASES.length) { ClientValidation.finish(mc, null); return; }
        }
        step++;
    }
    private static ItemStack item(String id) {
        var key = ResourceLocation.parse(id);
        require(BuiltInRegistries.ITEM.containsKey(key), "fixture item exists: " + id);
        return new ItemStack(BuiltInRegistries.ITEM.get(key));
    }
    private static void open(Minecraft mc, String name) throws Exception {
        mc.player.getInventory().clearContent();
        Class<?> type;
        AbstractContainerMenu menu;
        if (scenario < 2) {
            mc.player.getInventory().setItem(0, item("sophisticatedbackpacks:" + name));
            Class<?> contextType = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext");
            String handler = (String)Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider").getField("MAIN_INVENTORY").get(null);
            Object context = Class.forName(contextType.getName() + "$Item").getConstructor(String.class, int.class).newInstance(handler, 0);
            type = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer");
            menu = (AbstractContainerMenu)type.getConstructor(int.class, Player.class, contextType).newInstance(81, mc.player, context);
            screen = (AbstractContainerScreen<?>)Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen")
                    .getConstructor(type, Inventory.class, Component.class).newInstance(menu, mc.player.getInventory(), Component.literal("Animated Inventory / " + name));
        } else {
            BlockPos pos = mc.player.blockPosition().offset(2, 0, 2);
            var id = ResourceLocation.parse("sophisticatedstorage:" + name);
            require(BuiltInRegistries.BLOCK.containsKey(id), "fixture block exists: " + id);
            mc.level.setBlock(pos, BuiltInRegistries.BLOCK.get(id).defaultBlockState(), 3);
            type = Class.forName("net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu");
            menu = (AbstractContainerMenu)type.getConstructor(int.class, Player.class, BlockPos.class).newInstance(81, mc.player, pos);
            screen = (AbstractContainerScreen<?>)Class.forName("net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen")
                    .getMethod("constructScreen", type, Inventory.class, Component.class).invoke(null, menu, mc.player.getInventory(), Component.literal("Animated Inventory / " + name));
        }
        wrapper = menu.getClass().getMethod("getStorageWrapper").invoke(menu);
        mc.player.containerMenu = menu; mc.setScreen(screen);
    }
    private static Object installUpgrade(Minecraft mc, String id) throws Exception {
        var menu = screen.getMenu();
        int index = menu.slots.size();
        menu.setCarried(ItemStack.EMPTY);
        menu.setItem(index, menu.getStateId(), item("sophisticated" + (scenario < 2 ? "backpacks" : "storage") + ":" + id));
        menu.getClass().getMethod("setOpenTabId", int.class).invoke(menu, 0);
        Map<?, ?> containers = (Map<?, ?>)menu.getClass().getMethod("getUpgradeContainers").invoke(menu);
        require(containers.containsKey(0), "real upgrade container created: " + id);
        containers.get(0).getClass().getMethod("setIsOpen", boolean.class).invoke(containers.get(0), true);
        screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        return containers.get(0);
    }
    private static void click(Minecraft mc, Slot slot, int button, ClickType type) throws Exception {
        // Use the actual overridden screen handler, including its normal prediction and packet send.
        Method method = Class.forName("net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase")
                .getDeclaredMethod("slotClicked", Slot.class, int.class, int.class, ClickType.class);
        method.setAccessible(true);
        method.invoke(screen, slot, slot.index, button, type);
    }
    private static void motion(String id, int count) {
        var runtime = ClientRuntime.INSTANCE;
        var animation = runtime.animations.destination(id, System.nanoTime());
        require(animation != null, "moving representation exists: " + CASES[scenario] + " / " + id);
        require(animation.transition.stack().getCount() == count, "moving quantity retained: " + count);
        ItemStack actual = id.equals("cursor") ? screen.getMenu().getCarried() : screen.getMenu().getSlot(Integer.parseInt(id.substring(5))).getItem();
        int normal = AnimatedInventoryApi.normalRenderStack(screen, id, actual).getCount();
        require(normal + (animation.inline ? 0 : animation.transition.stack().getCount()) == actual.getCount(), "single rendering ownership: " + CASES[scenario] + " / " + id);
        require(animation.inline == inline, "expected rendering path: " + CASES[scenario]);
    }
    private static void capture(String name) { ClientValidation.captureSophisticated(screen, "sophisticated-" + CASES[scenario] + "-" + name); }
    private SophisticatedValidation() { }
}
