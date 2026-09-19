package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.api.AnimatedInventoryApi;
import com.cappleapple.animatedinventory.client.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import java.nio.file.*;
import java.util.*;

/** Opt-in runtime fixtures. Operates only the generated validation world; excluded from production artifacts. */
@EventBusSubscriber(modid = "animatedinventory", value = Dist.CLIENT)
public final class ClientValidation {
    private static boolean booted, done;
    private static int age, scenario = -1;
    private static long next, started = System.nanoTime();
    private static String screenshot;
    private static AbstractContainerScreen<?> testScreen;
    private static CustomViewFixture custom;
    private static AutoCloseable registration;
    private static final List<String> results = new ArrayList<>();
    private static final Path OUTPUT = Path.of("validation");
    private static final String[] CASES = {"inventory", "chest", "double_chest", "furnace", "crafting_table", "hopper",
            "barrel", "shulker_box", "beacon", "anvil", "smithing_table", "enchanting_table", "merchant", "horse", "creative"};
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("animatedinventory.validation") || done) return;
        var mc = Minecraft.getInstance();
        try {
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            mc.mouseHandler.releaseMouse();
            GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if (Boolean.getBoolean("animatedinventory.configValidation")) {
                configTick(mc); return;
            }
            if (System.nanoTime() - started > 240_000_000_000L) throw new IllegalStateException("Validation timed out");
            if (!booted && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                booted = true; mc.options.pauseOnLostFocus = false; mc.options.framerateLimit().set(120);
                mc.options.guiScale().set(2); mc.resizeDisplay();
                mc.createWorldOpenFlows().createFreshLevel("animatedinventory-validation-" + System.currentTimeMillis(),
                        new LevelSettings("Animated Inventory validation", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        new TitleScreen());
                return;
            }
            if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
            if (Boolean.getBoolean("animatedinventory.initialContentsValidation")) { InitialContentsValidation.tick(mc); return; }
            if (Boolean.getBoolean("animatedinventory.stashValidation")) { StashRecipeValidation.tick(mc); return; }
            if (Boolean.getBoolean("animatedinventory.transferValidation")) { TransferValidation.tick(mc); return; }
            if (Boolean.getBoolean("animatedinventory.sophisticatedValidation")) { SophisticatedValidation.tick(mc); return; }
            if (Boolean.getBoolean("animatedinventory.craftingValidation") && scenario >= 0) { craftingTick(mc); return; }
            if (Boolean.getBoolean("animatedinventory.bnsValidation") && scenario >= 0) { bnsTick(mc); return; }
            if (scenario < 0) {
                if (++age < 30) return;
                ClientConfig.MOVE_MS.set(600); ClientConfig.QUICK_MS.set(600); ClientConfig.PICKUP_MS.set(600);
                ClientConfig.OPEN_MS.set(500); ClientConfig.CLOSE_MS.set(500);
                mc.player.getInventory().clearContent();
                scenario = 0; age = 0;
                if (!Boolean.getBoolean("animatedinventory.bnsValidation") && !Boolean.getBoolean("animatedinventory.craftingValidation")) open(mc, CASES[scenario]);
                else { ClientConfig.MOVE_MS.set(1000); ClientConfig.QUICK_MS.set(1000); ClientConfig.PICKUP_MS.set(1000); ClientConfig.PLACE_MS.set(1000); }
                next = System.nanoTime() + 500_000_000; return;
            }
            if (System.nanoTime() < next) return;
            if (scenario == CASES.length) { customTick(mc); return; }
            next = System.nanoTime() + 250_000_000;
            age++;
            if (age == 1) {
                if (scenario == 0 && net.neoforged.fml.ModList.get().isLoaded("bundlednotsiloed")) {
                    require(mc.screen.getClass().getName().contains("bundlednotsiloed"), "BNS screen replacement is active");
                    require(ClientRuntime.INSTANCE.snapshot().items().values().stream()
                            .filter(v -> v.region().equals("player") && v.id().startsWith("slot:"))
                            .allMatch(v -> v.mayAnimate()), "BNS native player cells are eligible for animation");
                }
                require(ClientRuntime.INSTANCE.snapshot() != null, "snapshot created: " + CASES[scenario]);
                screenshot = CASES[scenario] + "-idle";
            } else if (age == 2 && scenario < 14) {
                // Seed a source and target, then perform a real vanilla menu transaction on the isolated client menu.
                var menu = testScreen.getMenu();
                int source = menu.slots.size() - 1, destination = menu.slots.size() - 2;
                menu.slots.get(source).set(new ItemStack(Items.OAK_PLANKS, 32));
                menu.slots.get(destination).set(new ItemStack(Items.OAK_PLANKS, 52));
                ClientRuntime.INSTANCE.afterInteraction();
                Slot target = menu.slots.get(destination);
                double px = (testScreen.getGuiLeft() + target.x + 8) * mc.getWindow().getGuiScale();
                double py = (testScreen.getGuiTop() + target.y + 8) * mc.getWindow().getGuiScale();
                GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), px, py);
                // Set fixture coordinates even when the dev window is not focused.
                var mouseX = MouseHandler.class.getDeclaredField("xpos"); mouseX.setAccessible(true); mouseX.setDouble(mc.mouseHandler, px);
                var mouseY = MouseHandler.class.getDeclaredField("ypos"); mouseY.setAccessible(true); mouseY.setDouble(mc.mouseHandler, py);
                ClientRuntime.INSTANCE.beforeInteraction(testScreen, menu.slots.get(source), ClickType.PICKUP);
                menu.clicked(source, 0, ClickType.PICKUP, mc.player);
                ClientRuntime.INSTANCE.afterInteraction();
                require(menu.getCarried().getCount() == 32, "pickup state immediate: " + CASES[scenario]);
                screenshot = CASES[scenario] + "-pickup";
            } else if (age == 3 && scenario < 14) {
                var menu = testScreen.getMenu(); Slot target = menu.slots.get(menu.slots.size() - 2);
                ClientRuntime.INSTANCE.beforeInteraction(testScreen, target, ClickType.PICKUP);
                menu.clicked(target.index, 0, ClickType.PICKUP, mc.player);
                ClientRuntime.INSTANCE.afterInteraction();
                require(target.getItem().getCount() == 64 && menu.getCarried().getCount() == 20, "partial merge immediate: " + CASES[scenario]);
                screenshot = CASES[scenario] + "-merge-highlight";
            } else if (age == 4) {
                testScreen.getMenu().setCarried(ItemStack.EMPTY);
                ClientRuntime.INSTANCE.invalidate(testScreen, false);
                screenshot = CASES[scenario] + "-hover-behind";
            } else if (age == 5) {
                long before = AnimatedInventoryApi.owner(mc.screen);
                mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                require(AnimatedInventoryApi.owner(mc.screen) != before, "resize renews owner: " + CASES[scenario]);
                require(ClientRuntime.INSTANCE.animations.active().isEmpty(), "resize clears ownership: " + CASES[scenario]);
                if (CASES[scenario].equals("chest") || CASES[scenario].equals("double_chest")) {
                    var menu = testScreen.getMenu(); int source = menu.slots.size() - 1;
                    menu.slots.get(0).set(new ItemStack(Items.IRON_INGOT, 52));
                    menu.slots.get(1).set(new ItemStack(Items.IRON_INGOT, 44));
                    menu.slots.get(source).set(new ItemStack(Items.IRON_INGOT, 32));
                    ClientRuntime.INSTANCE.invalidate(testScreen, false);
                    ClientRuntime.INSTANCE.beforeInteraction(testScreen, menu.slots.get(source), ClickType.QUICK_MOVE);
                    menu.clicked(source, 0, ClickType.QUICK_MOVE, mc.player);
                    ClientRuntime.INSTANCE.afterInteraction();
                    require(menu.slots.get(0).getItem().getCount() == 64 && menu.slots.get(1).getItem().getCount() == 64
                            && menu.slots.get(source).getItem().isEmpty(), "native quick-move splits 12/20: " + CASES[scenario]);
                    screenshot = CASES[scenario] + "-quick-move";
                }
            } else if (age == 6) {
                if (scenario == 1 && net.neoforged.fml.ModList.get().isLoaded("emi")) {
                    Class.forName("dev.emi.emi.api.EmiApi").getMethod("displayAllRecipes").invoke(null);
                    require(mc.screen != testScreen, "EMI public API opens a recipe screen");
                    results.add("PASS recipe viewer opened: " + mc.screen.getClass().getName());
                    LogUtils.getLogger().info("AI_VALIDATION recipe viewer {}", mc.screen.getClass().getName());
                }
                if (mc.screen == testScreen) mc.setScreen(new GenericMessageScreen(Component.literal("Validation: temporary screen")));
                require(ClientRuntime.INSTANCE.animations.active().isEmpty(), "screen removal clears animations: " + CASES[scenario]);
            } else if (age == 7) {
                mc.setScreen(testScreen);
                require(ClientRuntime.INSTANCE.snapshot() != null, "return refreshes snapshot: " + CASES[scenario]);
                screenshot = CASES[scenario] + "-returned";
            } else if (age == 8) {
                mc.setScreen(null);
            } else if (age == 9) {
                require(ClientRuntime.INSTANCE.animations.active().isEmpty(), "closed screen releases ownership: " + CASES[scenario]);
                scenario++; age = 0;
                if (scenario == CASES.length) {
                    mc.gameMode.setLocalMode(GameType.SURVIVAL);
                    registration = AnimatedInventoryApi.registerInventoryViewProvider(CustomViewFixture.PROVIDER);
                    custom = new CustomViewFixture(); mc.setScreen(custom); return;
                }
                open(mc, CASES[scenario]);
            }
        } catch (Throwable error) { finish(mc, error); }
    }

    /** Regression cases exercise actual native Slot renders with BNS installed, including non-hotbar endpoints. */
    private static void bnsTick(Minecraft mc) throws Exception {
        if (System.nanoTime() < next) return;
        next = System.nanoTime() + 300_000_000L;
        age++;
        var runtime = ClientRuntime.INSTANCE;
        if (age == 1) {
            require(net.neoforged.fml.ModList.get().isLoaded("bundlednotsiloed"), "BNS is installed");
            open(mc, "inventory");
            require(mc.screen.getClass().getName().contains("bundlednotsiloed"), "actual BNS inventory screen is open");
        } else if (age == 2) {
            Slot source = playerSlot(mc, 9);
            source.set(new ItemStack(Items.DIAMOND, 16));
            runtime.invalidate(testScreen, false);
            require(runtime.snapshot().items().get(VanillaInventoryProvider.id(source)).mayAnimate(), "main-grid source is eligible");
        } else if (age == 3) {
            click(mc, playerSlot(mc, 9), 0, ClickType.PICKUP);
            require(testScreen.getMenu().getCarried().getCount() == 16, "BNS main-grid pickup updates carried stack immediately");
            movement(VanillaInventoryProvider.id(playerSlot(mc, 9)), "cursor", "BNS main grid to cursor");
        } else if (age == 4) {
            click(mc, playerSlot(mc, 10), 0, ClickType.PICKUP);
            require(playerSlot(mc, 10).getItem().getCount() == 16, "BNS grid-to-grid placement updates immediately");
            movement("cursor", VanillaInventoryProvider.id(playerSlot(mc, 10)), "BNS cursor to main grid");
            screenshot = "bns-grid-placement";
        } else if (age == 5) {
            movement("cursor", VanillaInventoryProvider.id(playerSlot(mc, 10)), "BNS grid placement survives native render");
            screenshot = "bns-grid-placement-midflight";
        } else if (age == 7) {
            click(mc, playerSlot(mc, 10), 0, ClickType.SWAP);
            require(playerSlot(mc, 0).getItem().getCount() == 16, "BNS main-grid to hotbar state");
            movement(VanillaInventoryProvider.id(playerSlot(mc, 10)), VanillaInventoryProvider.id(playerSlot(mc, 0)), "BNS main grid to hotbar");
            screenshot = "bns-grid-to-hotbar";
        } else if (age == 8) {
            movement(VanillaInventoryProvider.id(playerSlot(mc, 10)), VanillaInventoryProvider.id(playerSlot(mc, 0)), "BNS hotbar movement survives native render");
            click(mc, playerSlot(mc, 11), 0, ClickType.SWAP);
            require(playerSlot(mc, 11).getItem().getCount() == 16, "BNS hotbar to main-grid state");
            movement(VanillaInventoryProvider.id(playerSlot(mc, 0)), VanillaInventoryProvider.id(playerSlot(mc, 11)), "BNS hotbar to main grid");
            screenshot = "bns-hotbar-to-grid";
        } else if (age == 9) {
            movement(VanillaInventoryProvider.id(playerSlot(mc, 0)), VanillaInventoryProvider.id(playerSlot(mc, 11)), "BNS grid movement survives native render");
            Object data = bnsData(mc);
            long oldLayout = runtime.snapshot().layoutRevision();
            data.getClass().getMethod("showInventoryRange", int.class).invoke(data, 18);
            runtime.afterInteraction();
            require(runtime.snapshot().layoutRevision() != oldLayout, "BNS acknowledged page changes layout revision");
            require(runtime.animations.active().isEmpty(), "BNS page change releases movement without fake transfers");
            data.getClass().getMethod("showInventoryRange", int.class).invoke(data, 9);
            runtime.afterInteraction();
            require(runtime.animations.active().isEmpty(), "BNS return page does not infer acquisition");
        } else if (age == 10) {
            screenshot = "bns-grid-hover-behind";
        } else if (age == 11) {
            open(mc, "chest");
        } else if (age == 12) {
            playerSlot(mc, 9).set(new ItemStack(Items.EMERALD, 16));
            runtime.invalidate(testScreen, false);
        } else if (age == 13) {
            click(mc, playerSlot(mc, 9), 0, ClickType.QUICK_MOVE);
            require(testScreen.getMenu().getSlot(0).getItem().getCount() == 16, "BNS main-grid quick-move reaches chest immediately");
            movement(VanillaInventoryProvider.id(playerSlot(mc, 9)), "slot:0", "BNS main grid to chest");
            screenshot = "bns-grid-to-chest";
        } else if (age == 14) {
            movement(VanillaInventoryProvider.id(playerSlot(mc, 9)), "slot:0", "BNS chest movement survives native render");
            screenshot = "bns-grid-to-chest-midflight";
        } else if (age == 16) {
            click(mc, testScreen.getMenu().getSlot(0), 0, ClickType.PICKUP);
            movement("slot:0", "cursor", "chest to cursor");
        } else if (age == 17) {
            click(mc, playerSlot(mc, 10), 0, ClickType.PICKUP);
            require(playerSlot(mc, 10).getItem().getCount() == 16, "chest-to-BNS-main-grid placement state");
            movement("cursor", VanillaInventoryProvider.id(playerSlot(mc, 10)), "chest cursor to BNS main grid");
            screenshot = "bns-chest-to-grid";
        } else if (age == 18) {
            movement("cursor", VanillaInventoryProvider.id(playerSlot(mc, 10)), "BNS destination movement survives native render");
            screenshot = "bns-chest-to-grid-midflight";
        } else if (age == 21) {
            require(runtime.animations.active().isEmpty(), "BNS completed movement releases ownership");
            require(AnimatedInventoryApi.normalRenderStack(testScreen, VanillaInventoryProvider.id(playerSlot(mc, 10)),
                    playerSlot(mc, 10).getItem()).getCount() == 16, "BNS destination regains normal rendering");
            mc.setScreen(null);
            require(runtime.animations.active().isEmpty(), "BNS close releases all ownership");
            open(mc, "chest");
        }

        else if (age == 22) {
            Object data = bnsData(mc);
            Object storage = data.getClass().getMethod("inventory").invoke(data);
            storage.getClass().getMethod("clear").invoke(storage);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object stowedFirst = Enum.valueOf((Class)Class.forName("com.cappleapple.bundlednotsiloed.inventory.NewItemDestination"), "STOWED_FIRST");
            data.getClass().getMethod("setNewItemDestination", stowedFirst.getClass()).invoke(data, stowedFirst);
            storage.getClass().getMethod("replaceSyntheticSlotFromItemUse", int.class, ItemStack.class)
                    .invoke(storage, 38, new ItemStack(Items.APPLE, 52));
            testScreen.getMenu().getSlot(0).set(new ItemStack(Items.APPLE, 32));
            runtime.invalidate(testScreen, false);
            require(runtime.snapshot().items().get("bns:stowed:38").offscreenDestination(), "stowed storage has an explicit arrival point");
            require(AnimatedInventoryApi.getLogicalBounds(testScreen, "bns:stowed:38").isEmpty(), "hidden destination is not reported as a visible slot");
        } else if (age == 23) {
            click(mc, testScreen.getMenu().getSlot(0), 0, ClickType.QUICK_MOVE);
            require(testScreen.getMenu().getSlot(0).getItem().isEmpty(), "stowed quick-move removes chest source immediately");
            require(runtime.snapshot().items().get("bns:stowed:38").stack().getCount() == 64, "stowed merge reaches 64");
            require(runtime.snapshot().items().get("bns:stowed:36").stack().getCount() == 20, "stowed overflow receives remaining 20");
            stowedMovement(32, "chest to stowed split");
            screenshot = "bns-stowed-split";
        } else if (age == 24) {
            stowedMovement(32, "stowed split survives actual render");
            screenshot = "bns-stowed-split-midflight";
        } else if (age == 25) {
            bnsData(mc).getClass().getMethod("showInventoryRange", int.class).invoke(bnsData(mc), 18);
            runtime.afterInteraction();
            require(runtime.animations.active().isEmpty(), "page replacement cancels active stowed transfers");
            require(!runtime.snapshot().items().containsKey("bns:stowed:38"), "visible stowed slots are excluded from hidden destinations");
            require(runtime.snapshot().items().get(VanillaInventoryProvider.id(playerSlot(mc, 29))).visible(), "scrolled stowed destination becomes a normal visible slot");
        } else if (age == 26) {
            open(mc, "inventory");
        } else if (age == 27) {
            Object data = bnsData(mc), storage = data.getClass().getMethod("inventory").invoke(data);
            storage.getClass().getMethod("clear").invoke(storage);
            data.getClass().getMethod("showInventoryRange", int.class).invoke(data, 9);
            playerSlot(mc, 9).set(new ItemStack(Items.DIAMOND, 16));
            runtime.invalidate(testScreen, false);
        } else if (age == 28) {
            Object data = bnsData(mc), storage = data.getClass().getMethod("inventory").invoke(data);
            require((boolean)storage.getClass().getMethod("stowMainGrid").invoke(storage), "BNS stow-main-grid transaction succeeds");
            runtime.afterInteraction();
            require(playerSlot(mc, 9).getItem().isEmpty(), "stow-main-grid releases source immediately");
            stowedMovement(16, "main grid to stowed");
            screenshot = "bns-stowed-grid";
        } else if (age == 29) {
            stowedMovement(16, "main-grid stow survives actual render");
            screenshot = "bns-stowed-grid-midflight";
        } else if (age == 30) {
            screenshot = "bns-stowed-grid-approach";
            next = System.nanoTime() + 150_000_000L;
        } else if (age == 31) {
            var animation = runtime.animations.active().stream().filter(a -> a.transition.type() == com.cappleapple.animatedinventory.api.animation.TransitionType.STOW).findFirst().orElseThrow();
            require(animation.alpha(System.nanoTime()) > .2 && animation.alpha(System.nanoTime()) < 1, "stowed item fades near grid edge");
            screenshot = "bns-stowed-grid-fade";
        } else if (age == 32) {
            require(runtime.animations.active().isEmpty(), "stowed arrival releases animation");
            ClientConfig.REDUCE_MOTION.set(true);
            playerSlot(mc, 10).set(new ItemStack(Items.EMERALD, 8));
            runtime.invalidate(testScreen, false);
            Object data = bnsData(mc), storage = data.getClass().getMethod("inventory").invoke(data);
            storage.getClass().getMethod("stowMainGrid").invoke(storage);
            runtime.afterInteraction();
            require(runtime.animations.active().isEmpty(), "reduced motion avoids off-page travel");
            ClientConfig.REDUCE_MOTION.set(false);
        } else if (age == 33) {
            playerSlot(mc, 11).set(new ItemStack(Items.GOLD_INGOT, 8));
            runtime.invalidate(testScreen, false);
            Object data = bnsData(mc), storage = data.getClass().getMethod("inventory").invoke(data);
            storage.getClass().getMethod("stowMainGrid").invoke(storage);
            runtime.afterInteraction();
            stowedMovement(8, "stowed movement before close");
            mc.setScreen(null);
            require(runtime.animations.active().isEmpty(), "close cancels stowed travel");
            finish(mc, null);
        }
    }

    private static void stowedMovement(int quantity, String label) {
        var runtime = ClientRuntime.INSTANCE;
        var movements = runtime.animations.active().stream()
                .filter(a -> a.transition.type() == com.cappleapple.animatedinventory.api.animation.TransitionType.STOW).toList();
        require(movements.stream().mapToInt(a -> a.transition.stack().getCount()).sum() == quantity, label + " conserves moved quantity");
        require(!movements.isEmpty() && movements.stream().noneMatch(a -> a.inline), label + " has a departing representation");
        Slot first = playerSlot(Minecraft.getInstance(), 9), last = playerSlot(Minecraft.getInstance(), 35);
        for (var a : movements) {
            var target = runtime.snapshot().items().get(a.transition.destinationId());
            require(target != null && target.offscreenDestination(), label + " uses confirmed hidden storage");
            require(a.destination.x() >= testScreen.getGuiLeft() + first.x && a.destination.x() <= testScreen.getGuiLeft() + last.x
                    && a.destination.y() == testScreen.getGuiTop() + last.y, label + " targets the bottom grid edge");
            require(!a.bounds(System.nanoTime()).equals(a.destination), label + " has visible travel");
        }
    }

    private static Slot playerSlot(Minecraft mc, int index) {
        return testScreen.getMenu().slots.stream().filter(s -> s.container == mc.player.getInventory()
                && s.getContainerSlot() == index).findFirst().orElseThrow();
    }
    private static Object bnsData(Minecraft mc) throws Exception {
        @SuppressWarnings("unchecked")
        var type = (java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<Object>>)
                Class.forName("com.cappleapple.bundlednotsiloed.data.ModAttachments").getField("PLAYER_DATA").get(null);
        return mc.player.getData(type);
    }
    private static void click(Minecraft mc, Slot slot, int button, ClickType type) throws Exception {
        // Keep the logical mouse away from both endpoints so a carried-stack transition has visible travel.
        double px = 40 * mc.getWindow().getGuiScale(), py = 40 * mc.getWindow().getGuiScale();
        var x = MouseHandler.class.getDeclaredField("xpos"); x.setAccessible(true); x.setDouble(mc.mouseHandler, px);
        var y = MouseHandler.class.getDeclaredField("ypos"); y.setAccessible(true); y.setDouble(mc.mouseHandler, py);
        var runtime = ClientRuntime.INSTANCE;
        runtime.beforeInteraction(testScreen, slot, type);
        testScreen.getMenu().clicked(slot.index, button, type, mc.player);
        runtime.afterInteraction();
    }
    private static void movement(String source, String destination, String label) {
        var runtime = ClientRuntime.INSTANCE;
        var animation = runtime.animations.active().stream().filter(a ->
                Objects.equals(a.transition.sourceId(), source) && Objects.equals(a.transition.destinationId(), destination))
                .findFirst().orElseThrow(() -> new AssertionError("missing animation: " + label));
        var logical = AnimatedInventoryApi.getLogicalBounds(testScreen, destination).orElseThrow();
        require(!animation.bounds(System.nanoTime()).equals(logical), label + " has animated travel");
        var target = runtime.snapshot().items().get(destination).stack();
        int drawn = AnimatedInventoryApi.normalRenderStack(testScreen, destination, target).getCount();
        require(drawn == (animation.inline ? target.getCount() : 0), label + " has one render owner");
        results.add("PASS " + label + (animation.inline ? " uses native inline rendering" : " uses animated overlay rendering"));
    }


    private static final String[] CRAFT_CASES = {"2x2-pickup", "2x2-shift", "3x3-pickup", "3x3-shift", "3x3-cake"};
    private static void craftingTick(Minecraft mc) throws Exception {
        if (System.nanoTime() < next) return;
        next = System.nanoTime() + 250_000_000L;
        age++;
        var runtime = ClientRuntime.INSTANCE;
        boolean table = scenario >= 2, quick = scenario == 1 || scenario == 3;
        if (age == 1) {
            open(mc, table ? "crafting_table" : "inventory");
            ClientConfig.CRAFT_MS.set(900); ClientConfig.PICKUP_MS.set(350);
            ClientConfig.MOVE_MS.set(350); ClientConfig.QUICK_MS.set(350);
            ClientConfig.OPEN.set(ClientConfig.ScreenEffect.NONE); ClientConfig.CLOSE.set(ClientConfig.ScreenEffect.NONE);
            LogUtils.getLogger().info("AI_VALIDATION crafting {}", CRAFT_CASES[scenario]);
        } else if (age == 2) {
            seedCraft(mc, scenario);
            runtime.invalidate(testScreen, false);
            require(runtime.animations.active().isEmpty(), "recipe preview alone has no ingredient flow: " + CRAFT_CASES[scenario]);
        } else if (age == 3) {
            click(mc, testScreen.getMenu().getSlot(0), 0, quick ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            assertCraftFlow(mc, "craft take: " + CRAFT_CASES[scenario]);
            screenshot = "crafting-" + CRAFT_CASES[scenario] + "-start";
        } else if (age == 4) {
            assertCraftFlow(mc, "after native render: " + CRAFT_CASES[scenario]);
            screenshot = "crafting-" + CRAFT_CASES[scenario] + "-flow";
        } else if (age == 5) {
            screenshot = "crafting-" + CRAFT_CASES[scenario] + "-approach";
        } else if (age == 7) {
            require(runtime.animations.active().isEmpty(), "crafting flow completes: " + CRAFT_CASES[scenario]);
            if (scenario == 4) {
                for (int i = 1; i <= 3; i++) require(testScreen.getMenu().getSlot(i).getItem().is(Items.BUCKET), "cake returns bucket in slot " + i);
            }
            // Arrange/refill the recipe without taking anything.
            testScreen.getMenu().setCarried(ItemStack.EMPTY);
            seedCraft(mc, scenario);
            runtime.afterInteraction();
            require(runtime.animations.active().stream().noneMatch(a -> a.transition.type() == com.cappleapple.animatedinventory.api.animation.TransitionType.CRAFT),
                    "refilling recipe does not craft: " + CRAFT_CASES[scenario]);
        } else if (age == 8) {
            runtime.invalidate(testScreen, false);
            ClientConfig.REDUCE_MOTION.set(true);
            click(mc, testScreen.getMenu().getSlot(0), 0, quick ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            require(runtime.animations.active().stream().noneMatch(a -> a.transition.type() == com.cappleapple.animatedinventory.api.animation.TransitionType.CRAFT),
                    "reduced motion skips ingredient travel: " + CRAFT_CASES[scenario]);
            ClientConfig.REDUCE_MOTION.set(false);
        } else if (age == 9) {
            testScreen.getMenu().setCarried(ItemStack.EMPTY);
            seedCraft(mc, scenario); runtime.invalidate(testScreen, false);
            click(mc, testScreen.getMenu().getSlot(0), 0, quick ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            require(runtime.animations.active().stream().anyMatch(a -> a.transition.type() == com.cappleapple.animatedinventory.api.animation.TransitionType.CRAFT),
                    "repeat craft animates: " + CRAFT_CASES[scenario]);
            mc.setScreen(null);
            require(runtime.animations.active().isEmpty(), "closing clears crafting flow: " + CRAFT_CASES[scenario]);
        } else if (age == 10) {
            scenario++; age = 0;
            if (scenario == CRAFT_CASES.length) finish(mc, null);
        }
    }
    private static Item craftedProduct(int scenario) {
        return switch (scenario) { case 0 -> Items.CRAFTING_TABLE; case 1 -> Items.OAK_PLANKS;
            case 2 -> Items.CHEST; case 3 -> Items.IRON_BLOCK; default -> Items.CAKE; };
    }
    private static void seedCraft(Minecraft mc, int scenario) {
        var menu = testScreen.getMenu();
        int size = scenario >= 2 ? 9 : 4;
        for (int i = 1; i <= size; i++) menu.getSlot(i).set(ItemStack.EMPTY);
        if (scenario == 0) for (int i = 1; i <= 4; i++) menu.getSlot(i).set(new ItemStack(Items.OAK_PLANKS, 3));
        else if (scenario == 1) menu.getSlot(1).set(new ItemStack(Items.OAK_LOG, 3));
        else if (scenario == 2) {
            for (int i = 1; i <= 9; i++) if (i != 5) menu.getSlot(i).set(new ItemStack(Items.OAK_PLANKS, 3));
        } else if (scenario == 3) for (int i = 1; i <= 9; i++) menu.getSlot(i).set(new ItemStack(Items.IRON_INGOT, 3));
        else {
            Item[] recipe = {Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET, Items.SUGAR, Items.EGG, Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT};
            for (int i = 0; i < recipe.length; i++) menu.getSlot(i + 1).set(new ItemStack(recipe[i]));
        }
        // Client menus receive result previews from the server. The fixture supplies that preview;
        // the actual menu click and ResultSlot ingredient/remnant logic run unchanged.
        menu.getSlot(0).set(new ItemStack(craftedProduct(scenario), scenario == 1 ? 4 : 1));
    }
    private static void assertCraftFlow(Minecraft mc, String label) {
        var runtime = ClientRuntime.INSTANCE;
        var flows = runtime.animations.active().stream().filter(a ->
                a.transition.type() == com.cappleapple.animatedinventory.api.animation.TransitionType.CRAFT).toList();
        int count = switch (scenario) { case 0 -> 4; case 1 -> 1; case 2 -> 8; default -> 9; };
        require(flows.size() == count, label + " has one flow per used ingredient");
        require(flows.stream().allMatch(a -> a.transition.stack().getCount() == 1 && !a.inline), label + " uses only consumed units");
        boolean quick = scenario == 1 || scenario == 3;
        require(flows.stream().allMatch(a -> quick ? !"slot:0".equals(a.transition.destinationId()) : "cursor".equals(a.transition.destinationId())),
                label + " follows crafted product");
        for (var a : flows) {
            require(!a.bounds(System.nanoTime()).equals(a.source), label + " has ingredient movement");
            var gridItem = runtime.snapshot().items().get(a.transition.sourceId());
            require(AnimatedInventoryApi.normalRenderStack(testScreen, gridItem.id(), gridItem.stack()).getCount() == gridItem.stack().getCount(),
                    label + " leaves remaining ingredients visible");
        }
        if (!quick) require(testScreen.getMenu().getCarried().is(craftedProduct(scenario)), label + " product is immediately carried");
    }

    private static void configTick(Minecraft mc) {
        if (!booted && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            booted = true; next = System.nanoTime() + 1_000_000_000L;
            mc.setScreen(new net.neoforged.neoforge.client.gui.ConfigurationScreen(
                    net.neoforged.fml.ModList.get().getModContainerById("animatedinventory").orElseThrow(), new TitleScreen()));
            screenshot = "configuration-screen";
        } else if (booted && System.nanoTime() >= next) {
            require(net.minecraft.client.resources.language.I18n.get("animatedinventory.configuration.general").equals("General"), "config categories are translated");
            require(net.minecraft.client.resources.language.I18n.get("animatedinventory.configuration.movement_duration_ms").equals("Movement duration"), "config options are translated");
            finish(mc, null);
        }
    }
    private static void customTick(Minecraft mc) throws Exception {
        next = System.nanoTime() + 250_000_000; age++;
        if (age == 1) {
            require(ClientRuntime.INSTANCE.snapshot().providerId().equals("animatedinventory:validation"), "custom provider selected");
            custom.reflow();
            require(!ClientRuntime.INSTANCE.animations.active().isEmpty(), "custom logical reflow animates");
            var logical = AnimatedInventoryApi.getLogicalBounds(custom, "fixture:wood").orElseThrow();
            var animated = AnimatedInventoryApi.getAnimatedBounds(custom, "fixture:wood").orElseThrow();
            require(!logical.equals(animated), "logical and animated coordinates differ during reflow");
            screenshot = "custom-api-reflow";
        } else if (age == 2) screenshot = "custom-api-middle";
        else if (age == 4) {
            require(ClientRuntime.INSTANCE.animations.active().isEmpty(), "custom animation completes");
            require(AnimatedInventoryApi.normalRenderStack(custom, "fixture:wood", new ItemStack(Items.OAK_PLANKS, 16)).getCount() == 16,
                    "custom view regains normal ownership");
            ClientConfig.REDUCE_MOTION.set(true); custom.appear(); custom.reflow();
            require(ClientRuntime.INSTANCE.animations.active().stream().allMatch(a -> a.duration <= 60_000_000), "reduced motion caps duration");
            screenshot = "custom-api-reduced-motion";
        } else if (age == 5) {
            ClientConfig.REDUCE_MOTION.set(false);
            mc.setScreen(new net.neoforged.neoforge.client.gui.ConfigurationScreen(
                    net.neoforged.fml.ModList.get().getModContainerById("animatedinventory").orElseThrow(), new TitleScreen()));
            require(AnimatedInventoryApi.getLogicalBounds(custom, "fixture:wood").isEmpty(), "removed custom screen queries are empty");
            require(ClientRuntime.INSTANCE.animations.active().isEmpty(), "config screen releases inventory animations");
            registration.close();
            screenshot = "configuration-screen";
        } else if (age == 7) {
            mc.setScreen(null); finish(mc, null);
        }
    }
    private static void open(Minecraft mc, String name) throws Exception {
        mc.gameMode.setLocalMode(GameType.SURVIVAL);
        Inventory inv = mc.player.getInventory(); inv.clearContent();
        if (name.equals("inventory")) testScreen = new InventoryScreen(mc.player);
        else if (name.equals("creative")) {
            mc.gameMode.setLocalMode(GameType.CREATIVE);
            testScreen = new CreativeModeInventoryScreen(mc.player, mc.level.enabledFeatures(), false);
        } else if (name.equals("horse")) {
            var horse = EntityType.HORSE.create(mc.level);
            var menu = new HorseInventoryMenu(81, inv, new SimpleContainer(17), horse, 0);
            testScreen = new HorseInventoryScreen(menu, inv, horse, 0);
        } else if (name.equals("chest") || name.equals("barrel") || name.equals("double_chest")) {
            var menu = name.equals("double_chest") ? ChestMenu.sixRows(81, inv) : ChestMenu.threeRows(81, inv);
            testScreen = new ContainerScreen(menu, inv, Component.literal("Animated Inventory / " + name));
        } else {
            String base = switch (name) {
                case "furnace" -> "Furnace"; case "crafting_table" -> "Crafting"; case "hopper" -> "Hopper";
                case "shulker_box" -> "ShulkerBox"; case "beacon" -> "Beacon"; case "anvil" -> "Anvil";
                case "smithing_table" -> "Smithing"; case "enchanting_table" -> "Enchantment"; default -> "Merchant";
            };
            Class<?> menuType = Class.forName("net.minecraft.world.inventory." + base + "Menu");
            AbstractContainerMenu menu = (AbstractContainerMenu)menuType.getConstructor(int.class, name.equals("beacon") ? net.minecraft.world.Container.class : Inventory.class).newInstance(81, inv);
            testScreen = (AbstractContainerScreen<?>)Class.forName("net.minecraft.client.gui.screens.inventory." + base + "Screen")
                    .getConstructor(menuType, Inventory.class, Component.class).newInstance(menu, inv, Component.literal("Animated Inventory / " + name));
        }
        testScreen.getMenu().setCarried(ItemStack.EMPTY);
        mc.player.containerMenu = testScreen.getMenu();
        mc.setScreen(testScreen);
        // BNS may replace the player screen through its supported screen event.
        if (mc.screen instanceof AbstractContainerScreen<?> actual) testScreen = actual;
        LogUtils.getLogger().info("AI_VALIDATION scenario {}", name);
    }
    @SubscribeEvent public static void frame(ScreenEvent.Render.Post event) {
        if (screenshot == null || done || !Boolean.getBoolean("animatedinventory.validation")) return;
        try {
            event.getGuiGraphics().flush();
            Files.createDirectories(OUTPUT);
            try (var pixels = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                pixels.writeToFile(OUTPUT.resolve(screenshot + ".png"));
                if (screenshot.equals("bns-stowed-grid-fade")) {
                    Slot first = playerSlot(Minecraft.getInstance(), 9), last = playerSlot(Minecraft.getInstance(), 35);
                    double scale = Minecraft.getInstance().getWindow().getGuiScale();
                    int x0 = (int)((testScreen.getGuiLeft() + first.x) * scale);
                    int y0 = (int)((testScreen.getGuiTop() + last.y) * scale), colored = 0;
                    for (int x = x0 + 2; x < x0 + (int)(15 * scale); x++) {
                        for (int y = y0 + 2; y < y0 + (int)(15 * scale); y++) {
                            int color = pixels.getPixelRGBA(x, y);
                            if ((color & 255) != ((color >>> 8) & 255)) colored++;
                        }
                    }
                    require(colored > 10, "fading stowed diamond remains visible in the actual framebuffer");
                }
            }
            screenshot = null;
            int error = GL11.glGetError();
            require(error == GL11.GL_NO_ERROR, "GL state clean: " + (scenario >= 0 && scenario < CASES.length ? CASES[scenario] : "custom-api"));
        } catch (Throwable error) { finish(Minecraft.getInstance(), error); }
    }
    static void captureSophisticated(AbstractContainerScreen<?> screen, String name) { testScreen = screen; screenshot = name; }
    static void require(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        results.add("PASS " + label);
    }
    static void finish(Minecraft mc, Throwable error) {
        done = true;
        if (error != null) { results.add("FAIL " + error); LogUtils.getLogger().error("AI_VALIDATION FAILED", error); }
        else results.add("PASS validation completed");
        try { Files.createDirectories(OUTPUT); Files.write(OUTPUT.resolve("results.txt"), results); } catch (Exception ignored) { }
        LogUtils.getLogger().info("AI_VALIDATION {}: {} checks", error == null ? "PASSED" : "FAILED", results.size());
        mc.stop();
    }
}
